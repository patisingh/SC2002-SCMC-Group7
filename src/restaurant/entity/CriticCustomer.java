package restaurant.entity;

import java.util.Comparator;

public class CriticCustomer extends Customer 
{

    public CriticCustomer(int id) 
    {
        super(id);
    }

    @Override
    protected int waitingPenalty()
    {
        return 12;
    }

    @Override
    public MenuItem chooseItem(Menu menu) 
    {
        return menu.getItems().stream()
                .max(Comparator.comparingInt(MenuItem::getPriceCents))
                .orElseThrow(() -> new IllegalStateException("The menu is empty."));
    }

    @Override
    public String getTypeName() 
    {
        return "Critic";
    }
}