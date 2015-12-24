
/*********************************************************************************
 * 
 * Class: Triangle
 * 
 * Description:
 * 
 * This class stores information about trading pair triangles.
 * 				
 ***********************************************************************************/

public class Triangle
{
	public static final double TRADE_FEE_PERCENT = 0.25;
	public static final double TRADE_FEE_MULTIPLIER = 0.992518734;//Math.pow((100.0 - TRADE_FEE_PERCENT) / 100.0, 3);
	
	private String baseCurrency;
	private int[] pairs = new int[3];
	
	private String tradeString = "";
	
	private TradeSeries bestSeries;
	
	public Triangle(String baseCurrency, int topPair, int middlePair, int bottomPair)
	{
		this.baseCurrency = baseCurrency;
		this.pairs[0] = topPair;
		this.pairs[1] = middlePair;
		this.pairs[2] = bottomPair;
	}
	
	public String toString()
	{
		String result = ("*** Triangle ***") + "\n" + 
						("Base currency pair: " + baseCurrency) + "\n" +
						("Top pair: " + pairs[0]) + "\n" +
						("Middle pair: " + pairs[1]) + "\n" + 
						("Bottom pair: " + pairs[2]);
		
		return result;
	}
	
	public String getTradeString()
	{
		return tradeString;
	}
	
	public void calculateProfit(TradingPair[] pairs)
	{
		tradeString = "";
		TradeSeries upwardSeries = getUpwardProfit(pairs);
		TradeSeries downwardSeries = getDownwardProfit(pairs);
		
		if(upwardSeries.getProfit() > downwardSeries.getProfit())
		{
			bestSeries = upwardSeries;
		}
		else
		{
			bestSeries = downwardSeries;
		}
	}
	
	private TradeSeries getUpwardProfit(TradingPair[] pairs)
	{
		return getDirectionProfit(pairs, 0, 1);
	}
	
	private TradeSeries getDownwardProfit(TradingPair[] pairs)
	{
		return getDirectionProfit(pairs, 2, -1);
	}
	
	private TradeSeries getDirectionProfit(TradingPair[] pairs, int start, int increment)
	{
		TradeSeries profitSeries = new TradeSeries();
		double amount = 100;
		
		int index = start;
		String startCurrency = this.baseCurrency;
		
		for(int i = 0; i < 3; i++)
		{
			amount = getAmountFromTrade(pairs[this.pairs[index]], amount, startCurrency, profitSeries);
			startCurrency = getNextStartCurrency(pairs[this.pairs[index]], startCurrency);
			index += increment;
		}
		
		amount *= TRADE_FEE_MULTIPLIER;
		profitSeries.setProfit(amount - 100.0);
		return profitSeries;
	}
	
	private double getAmountFromTrade(TradingPair pair, double amount, String startCurrency, TradeSeries profitSeries)
	{
		if(pair.getSecondCurrency().equals(startCurrency))
		{
			//buy
			profitSeries.addTrade(new Trade(pair.getMarketID(), "Buy"));
			
			//tradeString += ("Buy " + pair.getPairName() + " with " + startCurrency + " (" + pair.getAskPrice() + ")") + "\n";
			//tradeString += ("    Amount: " + amount + " => " + amount / pair.getAskPrice()) + "\n";
			amount = amount / pair.getAskPrice();
		}
		else
		{
			//sell
			profitSeries.addTrade(new Trade(pair.getMarketID(), "Sell"));
			
			//tradeString += ("Sell " + pair.getPairName() + " with " + startCurrency + " (" + pair.getBidPrice() + ")") + "\n";
			//tradeString += ("    Amount: " + amount + " => " + amount * pair.getBidPrice()) + "\n";
			amount = amount * pair.getBidPrice();
		}
		
		return amount;
	}
	
	private String getNextStartCurrency(TradingPair pair, String oldStart)
	{
		if(pair.getSecondCurrency().equals(oldStart))
		{
			return pair.getFirstCurrency();
		}
		else
		{
			return pair.getSecondCurrency();
		}
	}
	
	public int[] getPairs()
	{
		return pairs;
	}
	
	public double getBestProfit()
	{
		return bestSeries.getProfit();
	}
	
	public TradeSeries getBestTradeSeries()
	{
		return bestSeries;
	}
}