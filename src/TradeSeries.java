import java.util.ArrayList;
import java.util.Arrays;

import com.abwaters.cryptsy.Cryptsy;
import com.abwaters.cryptsy.Cryptsy.CryptsyException;

/*********************************************************************************
 * 
 * Class: TradeSeries
 * 
 * Description:
 * 
 * This class stores the trades for a specific direction of an arbitrage triangle.
 * 				
 ***********************************************************************************/

public class TradeSeries
{
	ArrayList<Trade> trades = new ArrayList<Trade>();
	private double profit;
	
	public TradeSeries()
	{
		
	}
	
	public void addTrade(Trade trade)
	{
		trades.add(trade);
	}
	
	public void setProfit(double profitIn)
	{
		profit = profitIn;
	}
	
	public double getProfit()
	{
		return profit;
	}
	
	public void setStartingAmount(Cryptsy cryptsy, TradingPair[] pairs)
	{
		double minStartingAmount = CryptsyArbitrage.MAX_TRADE_AMOUNT / pairs[trades.get(0).getMarketID()].getAskPrice();
		for(int trade = 0; trade < trades.size(); trade++)
		{
			double currentStartingAmount = getAmountForTrade(cryptsy, pairs, trade);
			System.out.println("Amount min: " + minStartingAmount + " vs current: " + currentStartingAmount);
			minStartingAmount = Math.min(minStartingAmount, currentStartingAmount);
		}
		System.out.println("Final amount: " + minStartingAmount);
		//System.exit(0);
		trades.get(0).setAmount(minStartingAmount * 0.35);
	}
	
	private double getAmountForTrade(Cryptsy cryptsy, TradingPair[] pairs, int tradeId)
	{
		Trade trade = trades.get(tradeId);
		double tradeAmount = 0.0;
		try
		{
			Cryptsy.DepthReturn data = cryptsy.getDepth(trade.getMarketID());
			Cryptsy.PriceQuantity[] depthArray;
			
			//System.out.println("depth sell: " + data.sell[0].price);
			//System.out.println(data.sell[0] + ", " + data.sell[1] + ", " + data.sell[2] + ", " + data.sell[3] + "...");
			//System.out.println("depth buy: " + data.buy[0].price);
			//System.out.println(data.buy[0] + ", " + data.buy[1] + ", " + data.buy[2] + ", " + data.buy[3] + "...");
			//System.exit(0);
			
			pairs[trade.getMarketID()].setTicker(data.buy[0].price, data.sell[0].price);
			if(trade.getDirection().equals("Buy"))
			{
				depthArray = data.sell;
			}
			else
			{
				depthArray = data.buy;
			}
			//System.out.println("Depth array: " + depthArray.length);
			for(int i = 0; i < depthArray.length && isWithinProfitBounds(depthArray[0].price, depthArray[i].price); i++)
			{
				tradeAmount += depthArray[i].quantity;
			}
		}
		catch (CryptsyException e)
		{
			System.out.println("Cryptsy Exception!");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(int i = tradeId; i > 0; i--)
		{	
			//jesus christ this sh*t is complex...
			Trade currentTrade = trades.get(i);
			TradingPair currentPair = pairs[currentTrade.getMarketID()];
			Trade previousTrade = trades.get(i - 1);
			TradingPair previousPair = pairs[previousTrade.getMarketID()];
			
			//System.out.println("Current pair: " + currentPair.getPairName());
			//System.out.println("Current amount: " + tradeAmount);
			//System.out.println("Previous pair: " + previousPair.getPairName());
			
			if(!currentPair.getFirstCurrency().equals(previousPair.getFirstCurrency()))
			{
				if(currentPair.getSecondCurrency().equals(previousPair.getFirstCurrency()))
				{
					tradeAmount *= currentPair.getAskPrice();
				}
				else if(currentPair.getFirstCurrency().equals(previousPair.getSecondCurrency()))
				{
					tradeAmount /= previousPair.getBidPrice();
				}
				else if(currentPair.getSecondCurrency().equals(previousPair.getSecondCurrency()))
				{
					tradeAmount *= currentPair.getAskPrice();
					tradeAmount /= previousPair.getBidPrice();
				}
				else
				{
					System.out.println("Impossible?!");
				}
			}
		}
		System.out.println("Final trade amount: " + tradeAmount + "\n");
		return tradeAmount;
	}
	
	private boolean isWithinProfitBounds(double bestPrice, double currentPrice)
	{
		return Math.abs(100 * (currentPrice - bestPrice) / bestPrice) <= (profit / 3.0);
	}
	
	public void executeTrades(Cryptsy cryptsy, TradingPair[] pairs)
	{
		//execute trades store in object
		for(int trade = 0; trade < trades.size(); trade++)
		{
			System.out.println("Executing trade: " + trades.get(trade).getDirection() + " " + pairs[trades.get(trade).getMarketID()].getPairName() +  "...");
			executeTrade(trades.get(trade), cryptsy, pairs);
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//System.exit(0);
	}
	
	private void executeTrade(Trade trade, Cryptsy cryptsy, TradingPair[] pairs)
	{
		executeTrade(trade, cryptsy, pairs, 0);
	}
	
	private void executeTrade(Trade trade, Cryptsy cryptsy, TradingPair[] pairs, int tries)
	{
		try
		{
			if(trade.getAmount() == -1)
			{
				double amount = 0;
				Cryptsy.InfoReturn info = cryptsy.getInfo();
				Cryptsy.Balances balances = info.balances_available;
				
				if(trade.getDirection().equals("Buy"))
				{
					String currencyNeeded = pairs[trade.getMarketID()].getSecondCurrency();
					amount = balances.get(currencyNeeded);
					amount /= pairs[trade.getMarketID()].getAskPrice() * 1.05;
				}
				else
				{
					String currencyNeeded = pairs[trade.getMarketID()].getFirstCurrency();
					amount = balances.get(currencyNeeded);
				}
				trade.setAmount(amount);
			}
			if(trade.getPrice() == -1)
			{
				double price;
				if(trade.getDirection().equals("Buy"))
				{
					price = pairs[trade.getMarketID()].getAskPrice();
					price *= 1.2;
				}
				else
				{
					//TODO use constants here
					price = pairs[trade.getMarketID()].getBidPrice();
					price *= 0.8;
				}
				trade.setPrice(price);
			}
			trade.execute(cryptsy);
			trade.setAmount(-1);
			trade.setPrice(-1);
		}
		catch(CryptsyException e)
		{
			if(tries <= 2)
			{
				executeTrade(trade, cryptsy, pairs, tries + 1);
			}
			else
			{
				System.out.println("Trade failed after 3 tries!");
				//e.printStackTrace();
			}
		}
	}
}