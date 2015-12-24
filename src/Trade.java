import com.abwaters.cryptsy.Cryptsy;
import com.abwaters.cryptsy.Cryptsy.CryptsyException;

/*********************************************************************************
 * 
 * Class: Trade
 * 
 * Description:
 * 
 * This class stores the data for a trade.
 * 				
 ***********************************************************************************/

public class Trade
{
	private int marketid;
	private double amount = -1;
	private double price = -1;
	private String direction;
	
	//private boolean completed = false;
	private long orderid;
	
	public Trade(int marketidIn, String directionIn)
	{
		marketid = marketidIn;
		direction = directionIn;
	}
	
	public void setAmount(double amountIn)
	{
		amount = amountIn;
	}
	
	public void setPrice(double priceIn)
	{
		price = priceIn;
	}
	
	public int getMarketID()
	{
		return marketid;
	}
	
	public double getAmount()
	{
		return amount;
	}
	
	public double getPrice()
	{
		return price;
	}
	
	public String getDirection()
	{
		return direction;
	}
	
	public boolean execute(Cryptsy cryptsy)
	{
		try
		{
			orderid = cryptsy.createOrder(marketid, direction, amount, price);
			return true;
		}
		catch (CryptsyException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean isCompleted(Cryptsy cryptsy)
	{
		try
		{
			Cryptsy.Trade[] trades = cryptsy.getMyTrades(marketid, 4);
			for(int trade = 0; trade < trades.length; trade++)
			{
				if(trades[trade].order_id == orderid)
				{
					return true;
				}
			}
			return false;
		}
		catch (CryptsyException e)
		{
			e.printStackTrace();
			return false;
		}
	}
}