package com.android.sendgrid;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;

import java.io.IOException;

public class MainActivity extends Activity {

  private static final String SENDGRID_USERNAME = "";
  private static final String SENDGRID_PASSWORD = "";
  private static final String SENDGRID_APIKEY = "";
  private static final int ADD_ATTACHMENT = 1;

  // Views
  private EditText toEditText;
  private EditText fromEditText;
  private EditText subjectEditText;
  private EditText msgEditText;
  private Button attachmentButton;
  private TextView attachmentText;
  private Button sendButton;

  // Attachment fields
  private Uri selectedAudioURI;
  private String attachmentName;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main);

    toEditText = (EditText) findViewById(R.id.to_editText);
    fromEditText = (EditText) findViewById(R.id.from_editText);
    subjectEditText = (EditText) findViewById(R.id.subject_editText);
    msgEditText = (EditText) findViewById(R.id.message_editText);
    attachmentButton = (Button) findViewById(R.id.attachment_button);
    attachmentText = (TextView) findViewById(R.id.attachment_textView);
    sendButton = (Button) findViewById(R.id.send_button);

    attachmentButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View view) {

        if (selectedAudioURI == null) {
          // Start get audio intent if no image to attach to email
          Intent intent = new Intent();
          //The Audio is saved as .mp4, can be found under the video intent
          intent.setType("video/*");
          intent.setAction(Intent.ACTION_GET_CONTENT);
          startActivityForResult(intent, ADD_ATTACHMENT);
        } else {
          // Remove audio attachment
          attachmentButton.setText("Add Attachment");
          attachmentText.setVisibility(View.GONE);
          selectedAudioURI = null;
        }
      }
    });

    sendButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        // Start send email ASyncTask
        SendEmailASyncTask task = new SendEmailASyncTask(getApplicationContext());
        task.execute();
      }
    });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == ADD_ATTACHMENT) {
      if(resultCode==RESULT_OK){
        // Get image from result intent
        selectedAudioURI = data.getData();
        ContentResolver contentResolver = getContentResolver();
        Log.d("SendAppExample", "Image Uri: " + selectedAudioURI);

        // Get image attachment filename
        attachmentName = "";
        String baseName = FilenameUtils.getBaseName(selectedAudioURI.toString());
        String extension = FilenameUtils.getExtension(selectedAudioURI.toString());
        attachmentName = baseName+"."+extension;

        // Update views to show attachment
        attachmentText.setVisibility(View.VISIBLE);
        attachmentText.setText(attachmentName);
        attachmentButton.setText("Remove Attachment");
      }
    }
  }

  /**
   * ASyncTask that composes and sends email
   */
  private class SendEmailASyncTask extends AsyncTask<Void, Void, Void> {

    private Context context;
    private String msgResponse;

    private SendEmailASyncTask(Context context) {
      this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {

      try {
        //SendGrid sendgrid = new SendGrid(SENDGRID_USERNAME, SENDGRID_PASSWORD);
        SendGrid sendgrid = new SendGrid(SENDGRID_APIKEY);

        SendGrid.Email email = new SendGrid.Email();

        // Get values from edit text to compose email
        // TODO: Validate edit texts
        email.addTo(toEditText.getText().toString());
        email.setFrom(fromEditText.getText().toString());
        email.setSubject(subjectEditText.getText().toString());
        email.setText(msgEditText.getText().toString());

        // Attach image
        if (selectedAudioURI != null) {
          email.addAttachment(attachmentName, getContentResolver().openInputStream(selectedAudioURI));
        }

        // Send email, execute http request
        SendGrid.Response response = sendgrid.send(email);
        msgResponse = response.getMessage();

        Log.d("SendAppExample", msgResponse);

      } catch (SendGridException e) {
        Log.e("SendAppExample", e.toString());
      } catch (JSONException e) {
        Log.e("SendAppExample", e.toString());
      } catch (IOException e) {
        Log.e("SendAppExample", e.toString());
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      Log.i("SendAppExample", msgResponse);
      Toast.makeText(context, msgResponse, Toast.LENGTH_LONG).show();
    }
  }
}
