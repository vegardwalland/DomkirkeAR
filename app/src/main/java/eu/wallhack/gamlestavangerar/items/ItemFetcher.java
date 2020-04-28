package eu.wallhack.gamlestavangerar.items;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ItemFetcher {

    private final String baseURL;
    private final RequestQueue queue;
    private final Gson gson;

    public ItemFetcher(String baseURL, RequestQueue queue) {
        this.queue = queue;
        this.baseURL = baseURL;
        gson = new Gson();
    }

    public CompletableFuture<Collection<Item>> getItems() {
        CompletableFuture<Collection<Item>> itemsFuture = new CompletableFuture<>();
        fetchItems(itemsFuture);
        return itemsFuture;
    }

    private void fetchItems(CompletableFuture<Collection<Item>> itemsFuture) {
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, baseURL + "/api/items?fields=_id", null,
                response -> fetchEachItem(itemsFuture, response),
                // TODO Error handling
                System.out::println
        );
        queue.add(request);
    }

    private void fetchEachItem(CompletableFuture<Collection<Item>> itemsFuture, JSONArray response) {
        ArrayList<CompletableFuture<Item>> itemRequests = new ArrayList<>();
        for (int i = 0; i < response.length(); i++) {
            try {
                String id = response.getJSONObject(i).getString("_id");
                itemRequests.add(fetchItem(id));
            } catch (JSONException e) {
                // TODO Error handling
                e.printStackTrace();
            }
        }

        // Once all fetches are complete, collect the items and complete the future.
        CompletableFuture.allOf(itemRequests.toArray(new CompletableFuture<?>[0])).whenComplete((aVoid, th) -> {
            if(th == null) {
                Collection<Item> items = itemRequests.stream().map(CompletableFuture::join).collect(Collectors.toList());
                itemsFuture.complete(items);
            }
        });
    }

    private CompletableFuture<Item> fetchItem(String id) {
        String requestURL = baseURL + "/api/items/" + id;
        CompletableFuture<Item> itemFuture = new CompletableFuture<>();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, requestURL, null,
                response -> itemFuture.complete(parseItem(response)),
                // TODO Error handling
                error -> {
                    itemFuture.exceptionally(null);
                }
        );
        queue.add(request);
        return itemFuture;
    }

    private Item parseItem(JSONObject response) {
        return gson.fromJson(response.toString(), Item.class);
    }

}
