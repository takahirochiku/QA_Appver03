package jp.techacademy.chiku.takahiro.qa_app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class QuestionDetailActivity extends AppCompatActivity {

    private ListView mListView;
    private Question mQuestion;
    private Question mName;
    private Question mQuestionUid;
    private Button mFavoriteButton;
    private QuestionDetailListAdapter mAdapter;

    private DatabaseReference mAnswerRef;
    private DatabaseReference mFavoritesRef;
    FirebaseAuth mAuth; //FirebaseAuthクラスを定義
    DatabaseReference mDataBaseReference; //データベースへの読み書きに必要なDatabaseReferenceクラスを定義

    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            String answerUid = dataSnapshot.getKey();

            for(Answer answer : mQuestion.getAnswers()) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid.equals(answer.getAnswerUid())) {
                    return;
                }
            }

            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");

            Answer answer = new Answer(body, name, uid, answerUid);
            mQuestion.getAnswers().add(answer);
            mAdapter.notifyDataSetChanged();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        // 渡ってきたQuestionのオブジェクトを保持する
        Bundle extras = getIntent().getExtras();
        mQuestion = (Question) extras.get("question");

        setTitle(mQuestion.getTitle());


        mDataBaseReference = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();  // FirebaseAuthのオブジェクトを取得する
        mFavoriteButton = (Button) findViewById(R.id.FavoriteButton); //リスナー登録する
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();  // ログイン済みのユーザーを取得する

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);

        mAdapter = new QuestionDetailListAdapter(this, mQuestion);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        if (user == null) {//ログインしていない
            mFavoriteButton.setVisibility(View.GONE); //ボタンを消す
        }else{
            mFavoriteButton.setVisibility(View.VISIBLE);
        }

        mFavoriteButton = (Button) findViewById(R.id.FavoriteButton);

        if (mQuestionUid == null) {
            mFavoriteButton.setText("お気に入り登録");
        }else{
            mFavoriteButton.setText("お気に入り解除");
        }

        mFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); //キーボードを隠す

                if (mQuestionUid == null) {

                    mDataBaseReference.child("users").child(String.valueOf(mName)).setValue(mQuestionUid);

                    //DatabaseReference favoriteRef = mDataBaseReference.child(Const.FavoritesPATH).child(String.valueOf(mName)).child(String.valueOf(mQuestionUid));
                    //ConstがFavoriteに、mName階層,QuestionUidを下階層にして登録？

                    //Map<String, String> data = new HashMap<String, String>();
                    //data.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());

                    Snackbar.make(findViewById(android.R.id.content), "登録しました", Snackbar.LENGTH_LONG).show();

                }else {

                    mDataBaseReference.child("users").child(String.valueOf(mName)).removeValue();

                    Snackbar.make(findViewById(android.R.id.content), "解除しました", Snackbar.LENGTH_LONG).show();
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ログイン済みのユーザーを取得する
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user == null) {
                    // ログインしていなければログイン画面に遷移させる
                    Intent intent = new Intent(getApplicationContext(), AnswerSendActivity.class);
                    intent.putExtra("question", mQuestion);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(getApplicationContext(), AnswerSendActivity.class);
                    intent.putExtra("question", mQuestion);
                    startActivity(intent);
                }
            }
        });

        DatabaseReference dataBaseReference = FirebaseDatabase.getInstance().getReference();
        mAnswerRef = dataBaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid()).child(Const.AnswersPATH);
        mAnswerRef.addChildEventListener(mEventListener);
    }
}