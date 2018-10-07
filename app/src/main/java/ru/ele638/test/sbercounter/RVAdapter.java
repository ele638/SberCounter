package ru.ele638.test.sbercounter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.RVVHolder> {

    private ArrayList<Message> messages;

    public void setMessages(ArrayList<Message> inMessages){
        this.messages = inMessages;
    }

    @NonNull
    @Override
    public RVVHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layout = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.card_layout, parent, false);

        return new RVVHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull RVVHolder holder, int position) {
        Message message = messages.get(position);
        holder.summ.setText(String.format(Locale.getDefault(), "%.2f руб.", message.SUMM));
        holder.date.setText(
                message.DATETIME == null ? "" :
                        String.format("%s", new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
                        .format(message.DATETIME)
                )
        );
        holder.place.setText(message.PLACE);
        holder.balance.setText(String.format(Locale.getDefault(), "Баланс %.2f руб.", message.BALANCE));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    //Custom holder class
    static class RVVHolder extends RecyclerView.ViewHolder {
        TextView summ, date, place, balance;

        RVVHolder(View itemView) {
            super(itemView);
            this.summ = itemView.findViewById(R.id.cv_summ);
            this.date = itemView.findViewById(R.id.cv_date);
            this.place = itemView.findViewById(R.id.cv_place);
            this.balance = itemView.findViewById(R.id.cv_balance);
        }
    }
}
