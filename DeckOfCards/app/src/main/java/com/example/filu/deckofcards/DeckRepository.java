package com.example.filu.deckofcards;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by filu on 02.05.17.
 */

public class DeckRepository implements IDeckRepository{
    IDeckPresenter presenter;
    String deckId = "";
    boolean deckNeedsReshuffling = false;//need this reference to know where to send the data once the async task is finished

    private final static String API_URL = "https://deckofcardsapi.com/api/deck/";

    @Override
    public void startGettingCards(int numDecks) {
        new DrawCardsTask().execute(numDecks);
    }

    @Override
    public void setPresenter(IDeckPresenter deckPresenter) {
        presenter = deckPresenter;
    }

    private class DrawCardsTask extends AsyncTask<Integer, Void, List<Card>> {


        @Override
        protected void onPostExecute(List<Card> cards) {
            super.onPostExecute(cards);
            presenter.receiveCards(cards);
        }

        @Override
        protected List<Card> doInBackground(Integer... params) {
            if(deckId.equals("")) { //drawing cards for the first time
                int numDecks = params[0];
                String newDeckId = getNewDeckId(numDecks);
                if(!newDeckId.equals("")) {
                    deckId = newDeckId;
                } else { //something wen't wrong on the API's side
                    return Collections.EMPTY_LIST;
                }
            }
            return getCards(deckId);
        }

        private String getNewDeckId(int numDecks) {
            String stringGetNewDeckURL = API_URL + "new/shuffle/?deck_count=" + Integer.toString(numDecks);
            try {
                URL getNewDeckURL = new URL(stringGetNewDeckURL);
                HttpURLConnection getNewDeckURLConnection = (HttpURLConnection) getNewDeckURL.openConnection();
                InputStream inputStream = getNewDeckURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuffer stringResponse = new StringBuffer(1024);
                String tmp = "";
                while((tmp= bufferedReader.readLine())!=null) {
                    stringResponse.append(tmp).append("\n");
                }

                bufferedReader.close();

                JSONObject deckJSON = new JSONObject(stringResponse.toString());
                if(deckJSON.getBoolean("success")) {
                    return deckJSON.getString("deck_id");
                } else {
                    return ""; //something went wrong with the API
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        private List<Card> getCards(String deckId) {
            if(!deckId.equals("")) { //just in case, dont really need to check it though
                String stringGetCardsUrl = API_URL + deckId + "/draw/?count=5";
                if(deckNeedsReshuffling) {
                    if(!reshuffleDeck(deckId)) { //something went wrong while reshuffling
                        return Collections.EMPTY_LIST;
                    }
                    deckNeedsReshuffling = false;
                }
                try {
                    URL getCardsUrl = new URL(stringGetCardsUrl);
                    HttpURLConnection getCardsURLConnection = (HttpURLConnection) getCardsUrl.openConnection();
                    InputStream inputStream = getCardsURLConnection.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    StringBuffer stringResponse = new StringBuffer(1024);
                    String tmp = "";
                    while ((tmp = bufferedReader.readLine()) != null) {
                        stringResponse.append(tmp).append("\n");
                    }
                    bufferedReader.close();

                    JSONObject cardsJson = new JSONObject(stringResponse.toString());

                    if(cardsJson.getBoolean("success")) {
                        if(cardsJson.getInt("remaining") < 5) {
                            deckNeedsReshuffling = true;
                        }
                        List<Card> resultCardList = new ArrayList<>();
                        JSONArray cardsArrayJSON = cardsJson.getJSONArray("cards");
                        for(int i = 0; i < cardsArrayJSON.length(); i++) {
                            JSONObject cardJSON = cardsArrayJSON.getJSONObject(i);
                            Drawable image = drawableFromUrl(cardJSON.getString("image")); //image field of the JSON holds url of cards image
                            String value = cardJSON.getString("value"); //eg. KING
                            String suit = cardJSON.getString("suit"); //eg. CLUBS
                            String code = cardJSON.getString("code"); //eg. KC

                            Card resultCard = new Card().setValue(value).setCode(code).setSuit(suit).setImage(image);
                            resultCardList.add(resultCard);
                        }
                        return resultCardList;
                    } else {
                        return Collections.EMPTY_LIST; //something went wrong with the API
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return Collections.EMPTY_LIST;
                }
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        private boolean reshuffleDeck(String deckId) {
            boolean reshuffledSuccessfully = false;
            String string_reshuffle_deck_url = API_URL + deckId + "/shuffle/";
            try {
                URL reshuffleDeckURL = new URL(string_reshuffle_deck_url);
                HttpURLConnection reshuffleDeckURLConnection = (HttpURLConnection) reshuffleDeckURL.openConnection();
                InputStream inputStream = reshuffleDeckURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuffer stringResponse = new StringBuffer(1024);
                String tmp = "";
                while ((tmp = bufferedReader.readLine()) != null) {
                    stringResponse.append(tmp).append("\n");
                }
                bufferedReader.close();

                JSONObject reshuffledDeckJSON = new JSONObject(stringResponse.toString());
                reshuffledSuccessfully = reshuffledDeckJSON.getBoolean("success");
                return reshuffledSuccessfully;
            } catch (Exception e) {
                e.printStackTrace();
                return reshuffledSuccessfully;
            }
        }


        private Drawable drawableFromUrl(String url) throws IOException {
            Bitmap x;

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.connect();
            InputStream input = connection.getInputStream();

            x = BitmapFactory.decodeStream(input);
            return new BitmapDrawable(x);
        }
    }
}
