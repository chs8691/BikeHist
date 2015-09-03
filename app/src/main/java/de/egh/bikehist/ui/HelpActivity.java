package de.egh.bikehist.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

import de.egh.bikehist.R;

public class HelpActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		setTitle(R.string.title_activity_help);

		WebView wv;
		wv = (WebView) findViewById(R.id.helpWebView);

		wv.loadUrl("file:///android_asset/" + getString(R.string.resourceHelpFile));
	}

}
