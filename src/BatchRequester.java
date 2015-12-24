import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*********************************************************************************
 * 
 * Class: BatchRequester
 * 
 * Description:
 * 
 * This class sends out a large number of requests in parallel to speed-up
 * 		program initialization time.
 * 				
 ***********************************************************************************/

public class BatchRequester
{
	private List<String> urls = new ArrayList<String>();
	
	private static class Result
	{
        private final String response;
        public Result(String response)
        {
            this.response = response;
        }
    }
	
	public void addRequest(String url)
	{
		urls.add(url);
	}
	
	public void execute(TradingPair[] pairs)
	{
		List<Callable<Result>> tasks = new ArrayList<Callable<Result>>();
        for (final String url : urls)
        {
            Callable<Result> c = new Callable<Result>()
            {
                @Override
                public Result call() throws Exception
                {
                    return urlGet(url);
                }
            };
            tasks.add(c);
        }

        ExecutorService exec = Executors.newCachedThreadPool();
        // some other exectuors you could try to see the different behaviours
        // ExecutorService exec = Executors.newFixedThreadPool(3);
        // ExecutorService exec = Executors.newSingleThreadExecutor();
        try
        {
            long start = System.currentTimeMillis();
            List<Future<Result>> results = exec.invokeAll(tasks);
            int sum = 0;
            for (Future<Result> fr : results)
            {
            	JSONObject data = null;
            	try
            	{
            		data = new JSONObject(fr.get().response);
            		//System.out.println("Decoded JSON");
            		//System.out.println(data.getInt("success"));
            	}
            	catch(JSONException e)
            	{
            	}
            	catch(java.lang.NullPointerException e)
            	{
            		data = null;
            	}
            	catch (InterruptedException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
        		if(data != null && data.getInt("success") == 1) {
        			data = data.getJSONObject("return");
        			data = data.getJSONObject("markets");
        			
        			String[] markets = JSONObject.getNames(data);
        			data = data.getJSONObject(markets[0]);
        			int id = data.getInt("marketid");
        			String firstCurrency = data.getString("primarycode");
        			String secondCurrency;
        			switch(id)
        			{
	        			case 450:	secondCurrency = "XRP";
	        						break;
	        			default:	secondCurrency = data.getString("secondarycode");
        			}
        			
        			TradingPair pair = new TradingPair(id, firstCurrency, secondCurrency);
        			
        			try
        			{
        				JSONArray bids = data.getJSONArray("buyorders");
	        			JSONObject bestBid = bids.getJSONObject(0);
	        			double bidPrice = bestBid.getDouble("price");
	        			//double bidAmount = bestBid.getDouble("quantity");
	        			
	        			JSONArray asks = data.getJSONArray("sellorders");
	        			JSONObject bestAsk = asks.getJSONObject(0);
	        			double askPrice = bestAsk.getDouble("price");
	        			//double askAmount = bestAsk.getDouble("quantity");
	        			
	        			pair.setTicker(bidPrice, askPrice);
	        			
	        			pairs[id] = pair;
	        			//System.out.println("Loaded market ID: " + id);
        			}
        			catch(JSONException e)
        			{
        				
        			}
        		}
            }
            long elapsed = System.currentTimeMillis() - start;
            //System.out.println(String.format("Elapsed time: %d ms", elapsed));
            //System.out.println(String.format("... but compute tasks waited for total of %d ms; speed-up of %.2fx", sum, sum / (elapsed * 1d)));
        }
        catch (InterruptedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} 
        catch (JSONException e) {
        	//
        }
        finally
        {
            exec.shutdown();
        }
	}
	
	public static Result urlGet(String targetURL) throws InterruptedException
	{
		URL url;
		HttpURLConnection connection = null;  
		try {
			//Create connection
			url = new URL(targetURL);
			connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("GET");//POST
			connection.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");

			/*connection.setRequestProperty("Content-Length", "" +
					Integer.toString(urlParameters.getBytes().length));*/
			connection.setRequestProperty("Content-Language", "en-US");  
				
			connection.setUseCaches (false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			//Send request
			DataOutputStream wr = new DataOutputStream (
					connection.getOutputStream ());
			/*wr.writeBytes (urlParameters);*/
			wr.flush ();
			wr.close ();

			//Get Response	
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer(); 
			while((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return new Result(response.toString());

		} catch (Exception e) {

			//e.printStackTrace();
			return null;
		
		} finally {

			if(connection != null) {
				connection.disconnect();
			}
		}
	}
}