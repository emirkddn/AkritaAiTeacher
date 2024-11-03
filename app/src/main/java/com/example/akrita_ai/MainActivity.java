package com.example.akrita_ai;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.huda_ai.R;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity implements ChatHistoryAdapter.OnChatHistoryClickListener {

    String lastQuestion;
    ImageButton btnAction;
    ImageButton btnAgain;
    EditText editTextText;
    List<Message> messageList;
    RecyclerView rv;
    MessageAdapter messageAdapter;
    StringBuilder conversationHistory = new StringBuilder();
    ConstraintLayout constraintLayout;
    ConstraintLayout constraintLayoutTitle;

    Layout myLinearLayout;

    DrawerLayout drawerLayout;
    ImageButton btnHist;

    //RecyclerView recyclerView;
    List<String> chatHistoryFiles;


    private static final int PERMISSION_REQUEST_CODE = 100;

    //public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        drawerLayout = findViewById(R.id.drawer_layout);
        //recyclerView = findViewById(R.id.recycler_view_chat_history);
        //recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        chatHistoryFiles = getChatHistoryFiles();
        ChatHistoryAdapter adapter = new ChatHistoryAdapter(chatHistoryFiles, this);
        //recyclerView.setAdapter(adapter);

        constraintLayout = findViewById(R.id.constraintLayout);
        constraintLayoutTitle = findViewById(R.id.constraintLayout2);

        //myLinearLayout = findViewById(R.id.left_chat_text_view);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        messageList = new ArrayList<>();

        btnAction = findViewById(R.id.btnAction);
        editTextText = findViewById(R.id.editTextText);
        rv = findViewById(R.id.rv);
        btnAgain = findViewById(R.id.btnAgain);
        btnHist = findViewById(R.id.btnHist);

        messageAdapter = new MessageAdapter(messageList);
        rv.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(getApplicationContext());
        llm.setStackFromEnd(true);
        rv.setLayoutManager(llm);
        firstquestion(getString(R.string.firstquestion));

        btnAgain.setVisibility(View.INVISIBLE);

        /*
        myLinearLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastClickTime < DOUBLE_CLICK_DELAY) {
                        myLinearLayout.setBackgroundColor(Color.RED);
                        lastClickTime = 0;
                        return true;
                    }
                    lastClickTime = currentTime;
                }
                return false;
            }
        });
       */


        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String question = editTextText.getText().toString().trim();
                addToChat(question, Message.SENT_BY_ME);
                editTextText.setText("");
                modelCall(question);
            }
        });

        btnAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addToChat(lastQuestion, Message.SENT_BY_ME);
                modelCall(lastQuestion);
            }
        });

        btnHist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    showUIComponents();
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                    hideUIComponents();
                }
            }
        });

        /*
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                checkUserPermission();

            }

            public void checkUserPermission() {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(new MainActivity(), new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                } else {
                    // Permission granted, proceed with your action
                }
            }
        });
        */

        editTextText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    btnAction.setVisibility(View.VISIBLE);
                    btnAgain.setVisibility(View.INVISIBLE);
                } else {
                    btnAction.setVisibility(View.INVISIBLE);
                    btnAgain.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    void hideUIComponents() {
        editTextText.setVisibility(View.GONE);
        btnAction.setVisibility(View.GONE);
        btnAgain.setVisibility(View.GONE);
        rv.setVisibility(View.GONE);
        constraintLayout.setVisibility(View.GONE);
        constraintLayoutTitle.setVisibility(View.GONE);
    }

    void showUIComponents() {
        editTextText.setVisibility(View.VISIBLE);
        btnAction.setVisibility(View.VISIBLE);
        btnAgain.setVisibility(View.VISIBLE);
        rv.setVisibility(View.VISIBLE);
        constraintLayout.setVisibility(View.VISIBLE);
        constraintLayoutTitle.setVisibility(View.VISIBLE);
    }


    public void loadChatHistoryToDrawerAgain() {
        chatHistoryFiles = getChatHistoryFiles();
        LinearLayout chatHistoryContainer = findViewById(R.id.chat_history_container);

        chatHistoryContainer.removeAllViews();

        for (String fileName : chatHistoryFiles) {
            Button button = new Button(this);
            button.setText(fileName);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String chatData = loadChatData(fileName);
                    Toast.makeText(MainActivity.this, "Sohbet Yüklendi:\n" + chatData, Toast.LENGTH_SHORT).show();
                }
            });

            chatHistoryContainer.addView(button);
        }
    }


    public void loadChatHistoryToDrawer() {
        DatabaseHelper db = new DatabaseHelper(this);
        Cursor res = db.getAllChats();
        List<String> chatHistory = new ArrayList<>();

        Log.d("ChatHistory", "Kayıt Sayısı: " + res.getCount());

        while (res.moveToNext()) {
            chatHistory.add(res.getString(1)); // 1. sütun CHAT
        }

        ChatHistoryAdapter adapter = new ChatHistoryAdapter(chatHistory, this);
        //recyclerView.setAdapter(adapter);
    }


    public void saveChatHistory(String chatData) {
        DatabaseHelper db = new DatabaseHelper(this);
        db.addChat(chatData);
    }

    public List<String> getChatHistoryFiles() {
        List<String> chatHistoryFiles = new ArrayList<>();
        File directory = new File(getFilesDir(), "chat_history");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    chatHistoryFiles.add(file.getName());
                }
            }
        }
        return chatHistoryFiles;
    }



    public void onChatHistoryClick(String fileName) {
        String chatData = loadChatData(fileName);
        Toast.makeText(this, "Sohbet Yüklendi:\n" + chatData, Toast.LENGTH_SHORT).show();
    }


    public String loadChatData(String fileName) {
        StringBuilder chatData = new StringBuilder();
        try {
            FileInputStream fis = openFileInput(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                chatData.append(line).append("\n");
            }
            reader.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chatData.toString();
    }


    public void closeChat() {
        String chatData = conversationHistory.toString();
        saveChatHistory(chatData);
        addChatButton(chatData);
        conversationHistory.setLength(0);
    }


    public void addChatButton(String chatData) {
        LinearLayout chatHistoryContainer = findViewById(R.id.chat_history_container);

        Button chatButton = new Button(this);
        chatButton.setText("Sohbet: " + (chatHistoryContainer.getChildCount() + 1));
        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Sohbet Yüklendi:\n" + chatData, Toast.LENGTH_SHORT).show();
            }
        });

        chatHistoryContainer.addView(chatButton);
    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(() -> {
            messageList.add(new Message(message, sentBy));
            messageAdapter.notifyDataSetChanged();
            rv.smoothScrollToPosition(messageAdapter.getItemCount());
        });
    }

    void addResponse(String response) {
        messageList.remove(messageList.size() - 1);
        addToChat(response, Message.SENT_BY_BOT);
    }

    public void firstquestion(String firstquestion){
        conversationHistory.append(firstquestion).append("\n");

        messageList.add(new Message("AI yükleniyor...", Message.SENT_BY_BOT));

        String fullContext = conversationHistory.toString();

        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", "API_KEY");
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content content = new Content.Builder().addText(fullContext).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                conversationHistory.append(resultText).append("\n");
                addResponse(resultText);
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                addResponse("Error: Unable to generate response.");
            }
        }, this.getMainExecutor());

    }


    public void modelCall(String question) {
        lastQuestion = question;

        conversationHistory.append(question).append("\n");

        messageList.add(new Message("Yazıyor...", Message.SENT_BY_BOT));

        String fullContext = conversationHistory.toString();

        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", "AIzaSyCE0XJfXBIf4vFIFNlrbHzkeSkWBFZAiyk");
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content content = new Content.Builder().addText(fullContext).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();

                /*
                SpannableString spannableString = new SpannableString(resultText);
                Pattern pattern = Pattern.compile("\\*\\*(.*?)\\*\\*");
                Matcher matcher = pattern.matcher(resultText);
                while (matcher.find()) {
                    int start = matcher.start(1);
                    int end = matcher.end(1);
                    spannableString.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                addResponse(spannableString);
                */

                // Append the response to the conversation history
                conversationHistory.append(resultText).append("\n");
                addResponse(resultText);
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                addResponse("Error: Unable to generate response.");
            }
        }, this.getMainExecutor());
    }


}