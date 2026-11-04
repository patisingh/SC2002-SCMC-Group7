package restaurant.entity;

public class VIPCustomer extends Customer 
{

    public VIPCustomer(int id) 
    {
        super(id);
    }

    @Override
    protected int waitingPenalty() 
    {
        return 5;
    }

    @Override
    public int billFor(MenuItem item) 
    {
        return Money.percentOf(item.getPriceCents(), 90);
    }

    @Override
    public String getTypeName() 
    {
        return "VIP";
    }
}