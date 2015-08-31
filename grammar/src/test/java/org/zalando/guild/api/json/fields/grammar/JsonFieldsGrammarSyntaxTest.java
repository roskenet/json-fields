package org.zalando.guild.api.json.fields.grammar;

import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.core.IsNot.not;

import static org.junit.Assert.assertThat;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.pattern.ParseTreePattern;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import org.junit.Test;

import org.zalando.guild.api.json.fields.grammar.parser.JsonFieldsLexer;
import org.zalando.guild.api.json.fields.grammar.parser.JsonFieldsParser;

/**
 * @author  Sean Patrick Floyd (sean.floyd@zalando.de)
 * @since   26.08.2015
 */
public class JsonFieldsGrammarSyntaxTest {

    static Matcher<String> aValidFieldsExpression() {
        return new TypeSafeDiagnosingMatcher<String>() {
            @Override
            protected boolean matchesSafely(final String fieldsExpression, final Description mismatchDescription) {
                try {

                    final JsonFieldsLexer lexer = new JsonFieldsLexer(new ANTLRInputStream(fieldsExpression));
                    lexer.removeErrorListeners();

                    final ANTLRErrorListener listener = new BaseErrorListener() {
                        @Override
                        public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol,
                                final int line, final int charPositionInLine, final String msg,
                                final RecognitionException e) {
                            throw new ParseCancellationException(e);
                        }

                    };
                    lexer.addErrorListener(listener);

                    final JsonFieldsParser parser = new JsonFieldsParser(new CommonTokenStream(lexer));
                    parser.removeErrorListeners();

                    parser.addErrorListener(listener);
                    parser.setErrorHandler(new BailErrorStrategy());

                    final ParseTreePattern parseTreePattern = parser.compileParseTreePattern(fieldsExpression, 0,
                            lexer);
                } catch (ParseCancellationException | RecognitionException | IllegalArgumentException e) {
                    mismatchDescription.appendValue(e);
                    return false;
                }

                return true;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("a valid field expression");
            }
        };
    }

    @Test
    public void simpleField() {
        assertThat("(foo)", is(aValidFieldsExpression()));
        assertThat("   (    foo    )   ", is(aValidFieldsExpression()));
        assertThat("(foo,bar)", is(aValidFieldsExpression()));
        assertThat("(foo, bar)", is(aValidFieldsExpression()));
    }

    @Test
    public void numerics() {
        assertThat("(foo123)", is(aValidFieldsExpression()));
        assertThat("   (    foo,bar123    )   ", is(aValidFieldsExpression()));
        assertThat("(f0o,b4r)", is(aValidFieldsExpression()));
    }

    @Test
    public void uppercase() {
        assertThat("(FOO)", is(aValidFieldsExpression()));
        assertThat("   (    fOo, bar, BAZ    )   ", is(aValidFieldsExpression()));
        assertThat("(  )", is(not(aValidFieldsExpression())));
    }

    @Test
    public void negation() {
        assertThat("!(foo)", is(aValidFieldsExpression()));
        assertThat("!foo", is(not(aValidFieldsExpression())));
        assertThat("!(foo, bar)", (is(aValidFieldsExpression())));
    }

    @Test
    public void qualifier() {
        assertThat("(foo(bar))", is(aValidFieldsExpression()));
        assertThat("(foo!(bar))", is(aValidFieldsExpression()));
        assertThat("foo(bar)", is(not(aValidFieldsExpression())));
    }

    @Test
    public void complexExpressions() {
        assertThat("(foo(bar(baz)))", is(aValidFieldsExpression()));
        assertThat("(foo!(bar(baz)))", is(aValidFieldsExpression()));
        assertThat("!(foo(bar(baz)))", is(aValidFieldsExpression()));
        assertThat("!(foo!(bar!(baz)))", is(aValidFieldsExpression()));
    }

    @Test
    public void syntaxError() {

        assertThat("", is(not(aValidFieldsExpression())));
        assertThat("foo, bar", is(not(aValidFieldsExpression())));
        assertThat("<<<foo", is(not(aValidFieldsExpression())));
    }
}