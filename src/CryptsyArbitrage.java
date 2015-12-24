import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.json.*;

import com.abwaters.cryptsy.Cryptsy;
import com.abwaters.cryptsy.Cryptsy.CryptsyException;
import com.justinschultz.pusherclient.*;
import com.justinschultz.pusherclient.Pusher.Channel;
import com.justinschultz.websocket.*;



/*********************************************************************************
 * 
 * Name:	John Reinstra
 * Date:	11/06/14
 * 	
 * Program : Cryptsy Arbitrage
 * Description:
 * 		This code finds triangular arbitrage opportunities on the Cryptsy exchange
 * 			and exploits these opportunities for a profit.
 * 				
 ***********************************************************************************/

public class CryptsyArbitrage
{
	
	public static final double MIN_PROFIT_PERCENT = -5;//0.50;
	public static final double MAX_TRADE_AMOUNT = 0.02;
	
	public static final int TIME_DELAY_SECS = 10;
	public static final int MESSAGE_FREQUENCY = 100;
	
	public static final int MAX_TRADING_PAIRS = 480;//480 (450, 458) => 449
	public static final String[] ALL_BASE_CURRENCIES = new String[] {"BTC", "LTC", "XRP", "USD"};
	public static final String[] CURRENT_BASE_CURRENCIES = new String[] {"BTC"};
	
	public static final String PUSH_APP_KEY = "cb65d0a7a72cd94adf1f";
	public static final String CRYPTSY_API_KEY = "1fff5290e4ab0464ef984b0c0dbec37889753cfa";
	public static final String CRYPTSY_API_SECRET = "d8703ce686d3b864749c09251a5ab4c0935d4c29031b51fc613f35bb357961a8164f965d17a933b3";
	
	private Triangle[] triangles = null;
	private TradingPair[] pairs = null;
	private Cryptsy cryptsy = new Cryptsy();
	
	private boolean isTrading = false;
	
	//private double reactionTime = 0;
	//private int iterations = 0;
	
	
	public static void main(String[] args)
	{
		CryptsyArbitrage gp = new CryptsyArbitrage();
		gp.init();
		gp.run();
	}
	
	public void init()
	{
		try
		{
			cryptsy.setAuthKeys(CRYPTSY_API_KEY, CRYPTSY_API_SECRET);
		}
		catch (CryptsyException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}
		loadTradingPairs();
		findTriangles();
		initializeWebsockets();
	}
	
	public void run()
	{
		for(int number = 1; true; number++)
		{
			System.out.println(number + ". Updating pairs...");
			updatePairs();
			updateTriangles();
			System.out.println("Updated.");
			
			if(!isTrading)
			{
				sortAndCheckTriangles(true);
			}
			try
			{
				Thread.sleep(TIME_DELAY_SECS * 1000);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void sortAndCheckTriangles(boolean outputInfo)
	{
		int highest = 0;
		for(int triangle = 0; triangle < triangles.length; triangle++)
		{
			double bestProfit = triangles[triangle].getBestProfit();
			if(bestProfit > 0 && bestProfit < 100)
			{
				//System.out.println(triangles[triangle].getTradeString() + "\n: profit: " + bestProfit);
			}
			if(bestProfit > triangles[highest].getBestProfit())
			{
				highest = triangle;
			}
		}
		loadTradingPairsRequest(triangles[highest].getPairs());
		triangles[highest].calculateProfit(pairs);
		if(outputInfo)
		{
			System.out.println("Highest profit: " + triangles[highest].getBestProfit());
		}
		if(!isTrading && triangles[highest].getBestProfit() > MIN_PROFIT_PERCENT)
		{
			isTrading = true;
			TradeSeries bestTradeSeries = triangles[highest].getBestTradeSeries();
			System.out.println("Sufficient profit: " + bestTradeSeries.getProfit());
			System.out.println("Beginning trade execution...");
			
			bestTradeSeries.setStartingAmount(cryptsy, pairs);
			bestTradeSeries.executeTrades(cryptsy, pairs);
			isTrading = false;
			//System.out.println(iterations + ". Would set amounts here for profit: " + tradeSeries.getProfit());
			//tradeSeries.setAmounts();
			//tradeSeries.executeTrades();
		}
		
		/*if(iterations % MESSAGE_FREQUENCY == 0 && !isTrading)
		{
			triangles[highest].calculateProfit(pairs);
			System.out.println("Highest profit: " + triangles[highest].getBestProfit());
			//System.out.println(triangles[highest]);
			//System.out.println(triangles[highest].getTradeString());
			//System.exit(0);
		}*/
	}
	
	private void updatePairs()
	{
		ArrayList<Integer> activePairs = new ArrayList<Integer>();
		for(int marketid = 0; marketid < pairs.length; marketid++)
		{
			if(pairs[marketid] != null)
			{
				activePairs.add(marketid);
			}
		}
		int[] activePairsArray = new int[activePairs.size()];
		for(int i = 0; i < activePairsArray.length; i++)
		{
			activePairsArray[i] = activePairs.get(i);
		}
		loadTradingPairsRequest(activePairsArray);
	}
	
	private void updateTriangles()
	{
		for(int triangle = 0; triangle < triangles.length; triangle++)
		{
			triangles[triangle].calculateProfit(pairs);
		}
	}
	
	private void loadTradingPairs()
	{
		System.out.println("Loading trading pairs...");
		
		pairs = new TradingPair[MAX_TRADING_PAIRS + 1];
		int[] marketids = new int[MAX_TRADING_PAIRS];
		for(int i = 0; i < marketids.length; i++)
		{
			marketids[i] = i;
		}
		loadTradingPairsRequest(marketids);
		
		System.out.println("Loaded.");
	}
	
	private void loadTradingPairsRequest(int[] marketids)
	{
		BatchRequester batch = new BatchRequester();
		for(int id = 0; id < marketids.length; id++)
		{
			batch.addRequest("http://pubapi.cryptsy.com/api.php?method=singlemarketdata&marketid=" + marketids[id]);
			//loadTradingPair(id);
			//System.out.println("Loaded marketid: " + id);
		}
		batch.execute(pairs);
	}
	
	private void findTriangles()
	{
		System.out.println("Loading triangles...");
		ArrayList<Triangle> result = new ArrayList<Triangle>();
		
		for(int base = 0; base < CURRENT_BASE_CURRENCIES.length; base++)
		{
			int[] firstPairs = getPairsWithSecond(CURRENT_BASE_CURRENCIES[base]);
			for(int firstPair = 0; firstPair < firstPairs.length; firstPair++)
			{
				//45 is there b/c of market issues
				//TODO find an automated way to ban markets like #45 (MEC_BTC) which has some sort of "phantom order" that messes with profit calculation
				if(!firstCurrencyIsABaseCurrency(firstPairs[firstPair]) && firstPairs[firstPair] != 45 && firstPairs[firstPair] != 59)
				{
					//System.out.println("First pair: " + firstPairs[firstPair]);
					String firstCurrency = pairs[firstPairs[firstPair]].getFirstCurrency();
					int[] secondPairs = getPairsWithFirst(firstCurrency);
					for(int secondPair = 0; secondPair < secondPairs.length; secondPair++)
					{
						//System.out.println("    Second pair: " + secondPairs[secondPair]);
						String secondCurrencyOfFirstPair = pairs[firstPairs[firstPair]].getSecondCurrency();
						String secondCurrencyOfSecondPair = pairs[secondPairs[secondPair]].getSecondCurrency();
						if(secondCurrencyOfFirstPair != secondCurrencyOfSecondPair)
						{
							int thirdPair = getPairWithFirstAndSecond(secondCurrencyOfSecondPair, secondCurrencyOfFirstPair);
							if(thirdPair != -1)
							{
								//System.out.println("        Third pair: " + thirdPair);
								
								Triangle newTriangle = new Triangle(
									CURRENT_BASE_CURRENCIES[base], 
									firstPairs[firstPair], 
									secondPairs[secondPair], 
									thirdPair
								);
								//System.out.println("Added triangle");
								result.add(newTriangle);
								int id = result.size() - 1;
								pairs[firstPairs[firstPair]].addTriangle(id);
								pairs[secondPairs[secondPair]].addTriangle(id);
								pairs[thirdPair].addTriangle(id);
							}
						}
					}
				}
			}
		}
		
		triangles = new Triangle[result.size()];
		for(int i = 0; i < result.size(); i++)
		{
			triangles[i] = result.get(i);
			triangles[i].calculateProfit(pairs);
		}
		
		System.out.println("Loaded.");
	}
	
	private boolean firstCurrencyIsABaseCurrency(int id)
	{
		String firstCurrency = pairs[id].getFirstCurrency();
		for(int i = 0; i < ALL_BASE_CURRENCIES.length; i++)
		{
			//TODO find out why this works (or not) without using correct string equality
			if(firstCurrency == ALL_BASE_CURRENCIES[i])
			{
				return true;
			}
		}
		
		return false;
	}
	
	private int[] getPairsWithFirst(String first)
	{
		return getPairsWithCurrency(0, first);
	}
	
	private int[] getPairsWithSecond(String second)
	{
		return getPairsWithCurrency(1, second);
	}
	
	private int[] getPairsWithCurrency(int index, String currencyName)
	{
		ArrayList<Integer> resultList = new ArrayList<Integer>();
		String pairCurrency;
		for(int pair = 0; pair < pairs.length; pair++)
		{
			if(pairs[pair] != null)
			{
				if(index == 0)
				{
					pairCurrency = pairs[pair].getFirstCurrency();
				}
				else if(index == 1)
				{
					pairCurrency = pairs[pair].getSecondCurrency();
				}
				else
				{
					return null;
				}
				
				if(pairCurrency.equals(currencyName))
				{
					resultList.add(pairs[pair].getMarketID());
				}
			}
		}
		
		int[] resultArray = new int[resultList.size()];
		for(int i = 0; i < resultList.size(); i++)
		{
			resultArray[i] = resultList.get(i).intValue();
		}
		
		return resultArray;
	}
	
	private int getPairWithFirstAndSecond(String first, String second)
	{
		for(int pair = 0; pair < pairs.length; pair++)
		{
			if(pairs[pair] != null)
			{
				if(pairs[pair].getFirstCurrency().equals(first) && pairs[pair].getSecondCurrency().equals(second))
				{
					return pairs[pair].getMarketID();
				}
			}
		}
		return -1;
	}
	
	private void initializeWebsockets()
	{
		final Pusher pusher = new Pusher(PUSH_APP_KEY);
		
		PusherListener eventListener = new PusherListener() {  

		    @Override
		    public void onConnect(String socketId) {
		        System.out.println("Pusher connected. Socket Id is: " + socketId);
		        for(int id = 1; id <= MAX_TRADING_PAIRS; id++)
				{
			        Channel channel = pusher.subscribe("ticker." + id);
			        //System.out.println("Subscribed to channel: " + channel);
	
			        channel.bind("message", new ChannelListener()
			        {
			            @Override
			            public void onMessage(String message)
			            {
			            	long startTime = System.nanoTime();
			                //System.out.println("Received bound channel message: " + message);
			                try
			                {
			                	//message = message.replace("\\", "");
			                	//System.out.println("Received bound channel message: " + message);
			                	JSONObject data = new JSONObject(message);
			                	String dataString = data.getString("data");
			                	data = new JSONObject(dataString);
			                	data = data.getJSONObject("trade");
			                	int id = data.getInt("marketid");
			                	
			        			JSONObject bestBid = data.getJSONObject("topbuy");
			        			double bidPrice = bestBid.getDouble("price");
			        			//double bidAmount = bestBid.getDouble("quantity");
			        			
			        			JSONObject bestAsk = data.getJSONObject("topsell");
			        			double askPrice = bestAsk.getDouble("price");
			        			//double askAmount = bestAsk.getDouble("quantity");
			        			
			        			if(pairs[id] != null)
			        			{
			        				pairs[id].setTicker(bidPrice, askPrice);
			        				ArrayList<Integer> pairTriangleIDs = pairs[id].getTriangles();
			        				for(int pairTriangleID = 0; pairTriangleID < pairTriangleIDs.size(); pairTriangleID++)
			        				{
			        					triangles[pairTriangleIDs.get(pairTriangleID)].calculateProfit(pairs);
			        				}
			        				if(!isTrading)
			        				{
			        					sortAndCheckTriangles(false);
			        				}
			        				long currentReactionTime = (System.nanoTime() - startTime) / 1000;
			        				//reactionTime = (currentReactionTime + (iterations * reactionTime)) / (iterations + 1);
			        				//iterations++;
			        				/*if(iterations % MESSAGE_FREQUENCY == 0 && !isTrading)
			        				{
			        					System.out.println("Reaction time: " + reactionTime + " µs");
			        					reactionTime = 0;
			        				} */ 
			        			}
			        			else
			        			{
			        				System.out.println("marketid: " + id + " is null, but it shouldn't be.");
			        			}
			                }
			                catch(JSONException e)
			                {
			                	// TODO  catch block
			                	//e.printStackTrace();alg
			                	//System.exit(0);
			                }
			            }
			        });
				}
		        System.out.println("Subscribed to market tickers.");
		    }

		    @Override
		    public void onMessage(String message)
		    {
		        //System.out.println("Received message from Pusher: " + message);
		    }

		    @Override
		    public void onDisconnect()
		    {
		        System.out.println("Pusher disconnected.");
		    }
		};
		
		pusher.setPusherListener(eventListener);
		pusher.connect();
	}
	
}