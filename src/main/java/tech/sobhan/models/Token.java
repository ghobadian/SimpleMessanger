package tech.sobhan.models;

import lombok.*;

import static tech.sobhan.utils.DataGenerator.generateToken;

@Builder
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Token {
    @Getter private final String token = generateToken();
    private final long startingTime = System.currentTimeMillis();

    public boolean checkExpiration(){
        if((System.currentTimeMillis() - startingTime) > 300_000){
            return true;
        }else{
            return false;
        }
    }
}
