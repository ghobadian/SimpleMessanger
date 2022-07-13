package tech.sobhan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;

@AllArgsConstructor
@Builder
public class Group {
    @Getter private String name;
    @Getter private final ArrayList<String> attendees = new ArrayList<>();

    public void addUser(String username){
        attendees.add(username);
    }
}
