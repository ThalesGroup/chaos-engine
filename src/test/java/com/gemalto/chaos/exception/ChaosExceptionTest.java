package com.gemalto.chaos;

import com.gemalto.chaos.exception.ChaosException;
import com.gemalto.chaos.exception.enums.ChaosErrorCode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@RunWith(MockitoJUnitRunner.class)
public class ChaosExceptionTest {
    private static final String MESSAGE = "This is why bad things happened";

    @Test(expected = ChaosException.class)
    public void emptyConstructor () {
        throw new ChaosException();
    }

    @Test(expected = ChaosException.class)
    public void stringConstructor () {
        ChaosException chaosException = new ChaosException(MESSAGE);
        assertEquals(MESSAGE, chaosException.getMessage());
        throw chaosException;
    }

    @Test(expected = ChaosException.class)
    public void stringThrowableConstructor () {
        Exception causeException = new Exception();
        ChaosException chaosException = new ChaosException(MESSAGE, causeException);
        assertEquals(MESSAGE, chaosException.getMessage());
        assertEquals(causeException, chaosException.getCause());
        throw chaosException;
    }

    @Test(expected = ChaosException.class)
    public void throwableConstructor () {
        Exception causeException = new Exception();
        ChaosException chaosException = new ChaosException(causeException);
        assertSame(causeException, chaosException.getCause());
        assertEquals(ChaosErrorCode.GENERIC_FAILURE.getFormattedMessage(), chaosException.getMessage());
        throw chaosException;
    }
}