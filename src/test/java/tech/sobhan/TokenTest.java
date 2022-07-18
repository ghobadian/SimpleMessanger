package tech.sobhan;

import tech.sobhan.models.Token;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class TokenTest {
    @Test
    public void tokenConstructorTest(){
        Token token1 = Token.builder().build();
        Token token2 = Token.builder().build();
        assertNotEquals(token1, token2);
    }
}
