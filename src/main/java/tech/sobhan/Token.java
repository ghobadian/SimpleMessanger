package tech.sobhan;

import static tech.sobhan.DataGenerator.generateToken;

public class Token {
    private String token;
    private long startingTime;

    public Token() {
        this.token = generateToken();;
        startingTime = System.currentTimeMillis();
    }

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

    public void setToken(String token) {
        this.token = token;
    }

    public long getStartingTime() {
        return startingTime;
    }

    public void setStartingTime(long startingTime) {
        this.startingTime = startingTime;
    }

    @Override
    public String toString() {
        return token;
    }
}
