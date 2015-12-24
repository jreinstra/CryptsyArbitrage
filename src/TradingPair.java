import java.util.ArrayList;

/*********************************************************************************
 * 
 * Class: TradingPair
 * 
 * Description:
 * 
 * This class stores information about trading pairs.
 * 				
 ***********************************************************************************/

public class TradingPair
{
	private int marketID;
	private String firstCurrency;
	private String secondCurrency;
	
	private double bidPrice;
	private double askPrice;
	
	private ArrayList<Integer> triangles = new ArrayList<Integer>();
	
	public TradingPair(int marketID, String firstCurrency, String secondCurrency)
	{
		this.marketID = marketID;
		this.firstCurrency = firstCurrency;
		this.secondCurrency = secondCurrency;
	}
	
	public void setTicker(double bidPrice, double askPrice)
	{
		this.bidPrice = bidPrice;
		this.askPrice = askPrice;
	}
	
	public void addTriangle(int id)
	{
		triangles.add(id);
	}
	
	public ArrayList<Integer> getTriangles()
	{
		return triangles;
	}
	
	public int getMarketID()
	{
		return marketID;
	}
	
	public String getFirstCurrency()
	{
		return firstCurrency;
	}
	
	public String getSecondCurrency()
	{
		return secondCurrency;
	}
	
	public double getBidPrice()
	{
		return bidPrice;
	}
	
	public double getAskPrice()
	{
		return askPrice;
	}
	
	public String toString()
	{
		String result = ("*** ID: " + marketID + " ***") +  "\n" + 
						("    Pair: " + firstCurrency + "_" + secondCurrency) + "\n" + 
						("Bid price: " + bidPrice) + "\n" + 
						("Ask price: " + askPrice);
		
		return result;
	}
	
	public String getPairName()
	{
		return firstCurrency + "_" + secondCurrency;
	}
}