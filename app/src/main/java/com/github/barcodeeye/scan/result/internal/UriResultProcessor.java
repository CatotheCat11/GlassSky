package com.github.barcodeeye.scan.result.internal;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.github.barcodeeye.scan.api.CardPresenter;
import com.github.barcodeeye.scan.result.ResultProcessor;
import com.google.zxing.Result;
import com.google.zxing.client.result.URIParsedResult;

public class UriResultProcessor extends ResultProcessor<URIParsedResult> {

	private static final String TAG = UriResultProcessor.class.getSimpleName();
	
    public UriResultProcessor(Context context, URIParsedResult parsedResult,
            Result result, Uri photoUri) {
        super(context, parsedResult, result, photoUri);
    }

    @Override
    public List<CardPresenter> getCardResults() {
        List<CardPresenter> cardResults = new ArrayList<CardPresenter>();

        URIParsedResult parsedResult = getParsedResult();

        CardPresenter cardPresenter = new CardPresenter()
                .setText("Open in Browser")
                .setFooter(parsedResult.getDisplayResult());

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(parsedResult.getURI()));
        
        Log.d(TAG, "Parsed URI: " + intent.getData());
        Log.d(TAG, "Intent: " + intent);
        
        cardPresenter.setPendingIntent(createPendingIntent(getContext(), intent));

        cardResults.add(cardPresenter);

        return cardResults;
    }

}
