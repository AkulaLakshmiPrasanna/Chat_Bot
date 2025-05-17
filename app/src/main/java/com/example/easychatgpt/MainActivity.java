package com.example.easychatgpt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    TextView welcomeTextView;
    EditText messageEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();
    private static final String API_KEY = "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";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welcome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);

        //setup recycler view
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        sendButton.setOnClickListener((v)->{
            String question = messageEditText.getText().toString().trim();
            addToChat(question,Message.SENT_BY_ME);
            messageEditText.setText("");
            callAPI(question);
            welcomeTextView.setVisibility(View.GONE);
        });
    }

    void addToChat(String message,String sentBy){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message,sentBy));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
            }
        });
    }

    void addResponse(String response){
        messageList.remove(messageList.size()-1);
        addToChat(response,Message.SENT_BY_BOT);
    }

    void callAPI(String question) {
        // Show typing indicator
        messageList.add(new Message("Typing... ", Message.SENT_BY_BOT));

        // Create the JSON request body with messages array
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "mixtral-8x7b-32768"); // Ensure model is correct

            // Prepare the messages array
            JSONArray messagesArray = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", question);
            messagesArray.put(userMessage);

            // Add messages array to the body
            jsonBody.put("messages", messagesArray);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Send the request using OkHttp
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions") // Correct GroqCloud endpoint
                .header("Authorization", "Bearer " + API_KEY) // Use your API key securely
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        // Log the raw response body for debugging
                        String responseBody = response.body().string();
                        System.out.println("Response Body: " + responseBody); // Log the raw response

                        // Try to parse the response JSON
                        JSONObject jsonObject = new JSONObject(responseBody);

                        // First check if the response contains "choices"
                        if (jsonObject.has("choices")) {
                            JSONArray jsonArray = jsonObject.getJSONArray("choices");

                            // Inspect the first object in the "choices" array
                            JSONObject choice = jsonArray.getJSONObject(0);

                            // Look for the "message" field if "text" is not present
                            if (choice.has("text")) {
                                String result = choice.getString("text").trim();
                                addResponse(result); // Update UI with the parsed result
                            } else if (choice.has("message")) {
                                // Handle cases where the result is under "message" or another field
                                JSONObject messageObject = choice.getJSONObject("message");
                                String result = messageObject.getString("content").trim(); // Adjust if needed
                                addResponse(result); // Update UI with the parsed result
                            } else {
                                // Handle if neither "text" nor "message" fields exist
                                addResponse("Unexpected response format: " + responseBody);
                            }
                        } else {
                            // If "choices" isn't available, handle accordingly
                            addResponse("Unexpected response format: " + responseBody);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        addResponse("Failed to parse response: " + e.getMessage());
                    }
                } else {
                    String responseBody = response.body().string(); // Extract the body as a string
                    addResponse("Failed to load response due to: " + responseBody);
                }
            }
        });
    }
        }



















