package eu.wallhack.domkirkear.items;


import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.javalite.http.Get;
import org.javalite.http.Http;

import java.util.ArrayList;
import java.util.Collection;

public class ItemFetcher {

    public static void main(String[] args) {
        ItemFetcher itemFetcher = ItemFetcher.makeItemFetcher("http://localhost:3000");
        Collection<Item> items = itemFetcher.fetchItems();
        for (Item item : items) {
            System.out.println(item);
        }
    }

    private String baseURL;
    private Gson gson;


    private ItemFetcher(String baseURL) {
        this.baseURL = baseURL;
        gson = new Gson();
    }

    public static ItemFetcher makeItemFetcher(String baseURL) {
        return new ItemFetcher(baseURL);
    }

    public ArrayList<Item> fetchItems() {

        ArrayList<Item> items = new ArrayList<>();
        JsonObject[] itemsWithOnlyIDs = fetchItemsWithOnlyIDs();

        // Use IDs to request the other details.
        for (JsonObject itemWithOnlyID : itemsWithOnlyIDs) {
            String id = itemWithOnlyID.get("_id").getAsString();
            // TODO There's another way to join the elements of the URL
            Item item = fetchItem(id);
            items.add(item);
        }

        return items;
    }

    private Item fetchItem(String id) {
        Get singleItemRequest = Http.get(baseURL + "/api/items/" + id);
        return gson.fromJson(singleItemRequest.text(), Item.class);
    }

    private JsonObject[] fetchItemsWithOnlyIDs() {
        // Get all the IDs
        Get itemListRequest = Http.get(baseURL + "/api/items?fields=_id");
        return gson.fromJson(itemListRequest.text(), JsonObject[].class);
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }
}


/*
public class ItemFetcher {

    public Collection<Item> getItems() {

        if (!connectedToInternet()) {
            // If not connected to internet, at least you can use the local items
            if (hasLocalItems()) {
                return getLocalItems();
            } else {
                // If there's no internet and no local items, the application can't be used.
                throw NoLocalItemsException();
            }
        }

        if (!hasLocalChecksums()) {
            // Remote items have never been fetched
            fetchAllRemoteItems();
            return getLocalItems();
        }

        return items;
    }
}*/
