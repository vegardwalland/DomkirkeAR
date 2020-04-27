package eu.wallhack.gamlestavangerar.items;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ItemFetcherTest {
    // TODO Mock instead of making an actual request
    private static final String BASE_URL = "https://domkirke.herokuapp.com";

    @Test
    public void itemsAreParsedProperlyTest() throws ExecutionException, InterruptedException {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RequestQueue queue = Volley.newRequestQueue(appContext);
        ItemFetcher itemFetcher = new ItemFetcher(BASE_URL, queue);

        Collection<Item> items = itemFetcher.getItems().get();

        assertNotNull(items);
    }
}
