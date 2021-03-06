package jp.techacademy.chiku.takahiro.qa_app;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.HashMap;

public class FavoritesActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private int mGenre = 0;
    private String mUserId;
    private DatabaseReference mDatabaseReference;
    private DatabaseReference mFavoritesRef;
    private ListView mListView;
    private ArrayList<Question> mQuestionArrayList;
    private ArrayList<String> mFavoritesArrayList;
    private QuestionsListAdapter mAdapter;

    private ChildEventListener mFavoritesListner = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            HashMap map = (HashMap) dataSnapshot.getValue();
            String favoriteQid = (String) map.get("favoriteQid");
            mFavoritesArrayList.add(favoriteQid);

        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
        }
    };

    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            for (String favoriteQid : mFavoritesArrayList) {
                if (dataSnapshot.getKey().equals(favoriteQid)) {
                    String title = (String) map.get("title");
                    String body = (String) map.get("body");
                    String name = (String) map.get("name");
                    String uid = (String) map.get("uid");
                    String imageString = (String) map.get("image");
                    byte[] bytes;
                    if (imageString != null) {
                        bytes = Base64.decode(imageString, Base64.DEFAULT);
                    } else {
                        bytes = new byte[0];
                    }

                    ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
                    HashMap answerMap = (HashMap) map.get("answers");
                    if (answerMap != null) {
                        for (Object key : answerMap.keySet()) {
                            HashMap temp = (HashMap) answerMap.get((String) key);
                            String answerBody = (String) temp.get("body");
                            String answerName = (String) temp.get("name");
                            String answerUid = (String) temp.get("uid");
                            Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                            answerArrayList.add(answer);
                        }
                    }

                    Question question = new Question(title, body, name, uid, dataSnapshot.getKey(), mGenre, bytes, answerArrayList);
                    mQuestionArrayList.add(question);
                    mAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            // 変更があったQuestionを探す
            for (Question question : mQuestionArrayList) {
                if (dataSnapshot.getKey().equals(question.getQuestionUid())) {
                    // このアプリで変更がある可能性があるのは回答(Answer)のみ
                    question.getAnswers().clear();
                    HashMap answerMap = (HashMap) map.get("answers");
                    if (answerMap != null) {
                        for (Object key : answerMap.keySet()) {
                            HashMap temp = (HashMap) answerMap.get((String) key);
                            String answerBody = (String) temp.get("body");
                            String answerName = (String) temp.get("name");
                            String answerUid = (String) temp.get("uid");
                            Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                            question.getAnswers().add(answer);
                        }
                    }

                    mAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        //intentでUserIDを持ってくる
        Bundle extras = getIntent().getExtras();
        mUserId = extras.getString("userid");

        //getReferanceの処理
        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);

        //Adapterの初期化
        mAdapter = new QuestionsListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();
        mFavoritesArrayList = new ArrayList<String>();

        // Qurestionarraylist初期化
        // Favorritesarraylist初期化
        //Adapterを作る(ListViewにsetAdapter()する）
        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mListView.setAdapter(mAdapter);

        //Contentsからデータを持ってくる
        mFavoritesRef = mDatabaseReference.child(Const.FavoritesPATH).child(mUserId);

        // mFavoritesRefを実行
        mFavoritesRef.addChildEventListener(mFavoritesListner);

        //adapterにsetList()する
        mAdapter.setQuestionArrayList(mQuestionArrayList);

        DatabaseReference mGenreRef;
        for (int i = 1; i <= 4; i++) {
            mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(i));
            mGenreRef.addChildEventListener(mEventListener);
        }

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ
                    if (mGenre == 0) {
                        Snackbar.make(view, "ジャンルを選択してください", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    // ログイン済みのユーザーを取得する
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        // ログインしていなければログイン画面に遷移させる
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                    } else {
                        // ジャンルを渡して質問作成画面を起動する
                        Intent intent = new Intent(getApplicationContext(), QuestionSendActivity.class);
                        intent.putExtra("genre", mGenre);
                        startActivity(intent);
                    }
                }
            });
            // ナビゲーションドロワーの設定
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name);
            drawer.addDrawerListener(toggle);
            toggle.syncState();
            // Listviewの準備　find by id Listの名前
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(MenuItem item) {
                    int id = item.getItemId();
                    if (id == R.id.nav_hobby) {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("genre", 1);
                        startActivity(intent);
                    } else if (id == R.id.nav_life) {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("genre", 2);
                        startActivity(intent);
                    } else if (id == R.id.nav_health) {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("genre", 4);
                        startActivity(intent);
                    } else if (id == R.id.nav_compter) {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("genre", 3);
                        startActivity(intent);
                    } else if (id == R.id.nav_favorites) {
                        mToolbar.setTitle("お気に入り");
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user == null) {
                            // ログインしていなければログイン画面に遷移させる
                            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                            startActivity(intent);
                        }
                    }
                    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    drawer.closeDrawer(GravityCompat.START);

                    // 選択したジャンルにリスナーを登録する
                    /*if (mGenreRef != null) {
                        mGenreRef.removeEventListener(mEventListener);
                    }
                    mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(mGenre));
                    mGenreRef.addChildEventListener(mEventListener);
                    return true;*/

                    return true;
                }
            });
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // Questionのインスタンスを渡して質問詳細画面を起動する
                    Intent intent = new Intent(getApplicationContext(), QuestionDetailActivity.class);
                    intent.putExtra("question", mQuestionArrayList.get(position));
                    startActivity(intent);
                }
            });
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();
            //noinspection SimplifiableIfStatement
            if (id == R.id.action_settings) {
                Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
                startActivity(intent);
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }