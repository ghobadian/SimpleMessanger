package tech.sobhan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static tech.sobhan.DataGenerator.generateToken;

@Builder
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Token {
    private final String token = generateToken();
    private final long startingTime = System.currentTimeMillis();

//    public Token() {
//        startingTime = ;
//    }

    public boolean checkExpiration(){
        if((System.currentTimeMillis() - startingTime) > 300_000){
            return true;
        }else{
            return false;
        }
    }

    public String getToken() {
        return token;
    }
}
