import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.telegram.TelegramConstants;
import org.apache.camel.component.telegram.TelegramParseMode;
import org.apache.camel.model.dataformat.JsonLibrary;

import java.util.Random;

/**
 * FOAAS - www.foaas.com
 * Integration with Telegram - run with:
 * kamel -d camel-hystrix run Foaas.java -p token=<bot token> --dev
 */
public class Foaas extends RouteBuilder {

    /**
     * list of method calls
     */
    private static String[] list = {"yoda", "this", "sake", "rtfm", "ratarse", "programmer", "tucker", "question", "no", "horse", "me", "asshole", "awesome", "bag", "because", "bucket", "bye", "cool", "cup", "diabetes", "everyone", "everything", "family", "fascinating", "flying", "fyyff", "give", "immensity", "life", "looking", "maybe", "mornin", "pink", "retard", "ridiculous", "shit", "single", "thanks", "that", "this", "too", "tucker", "what", "zayn", "zero"};

    /**
     * Camel Routes
     *
     * @throws Exception
     */
    public void configure() throws Exception {

        //@formatter:off
        from("telegram:bots/{{token}}")
                .id("main")
                //.to("log:DEBUG?showBody=true&showHeaders=true")
                // convert telegram input to string
                .convertBodyTo(String.class)
                // need this in separate class else Random evaluated only once
                .process(exchange -> {
                    exchange.getIn().setHeader("fMethod", list[new Random().nextInt(list.length)]);
                })
                .choice()
                    .when(simple("${body.toLowerCase()} contains 'mike'"))
                        // setup headers to call foaas service
                        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                        .setHeader(Exchange.HTTP_PATH, simple("${header.fMethod}/${body.toLowerCase()}"))
                        .setHeader("Accept", constant("application/json"))
                        // call
                        .to("direct:foaas")
                        .to("direct:telegram")
                        .log("${body}")
                    .otherwise()
                        // Just log all other messages
                        .log("Discarded: ${body}");

        from("direct:foaas")
                .id("foaas")
                // timeout if service not available
                .hystrix().hystrixConfiguration().executionTimeoutInMilliseconds(2000).end()
                    .to("https4://www.foaas.com")
                    // response in json
                    .unmarshal().json(JsonLibrary.Jackson)
                    // print back sensibly
                    .transform(simple("${body[message]} ${body[subtitle]}"))
                .onFallback()
                    .setBody(constant("FOAAS will swear at you shortly!"))
                .end();

        //@formatter:on

        from("direct:telegram")
                .id("telegram")
                // Let's retry up to 5 times if something goes wrong
                .errorHandler(defaultErrorHandler().maximumRedeliveries(5).redeliveryDelay(1000))
                // Source is HTML encoded, so we tell the Telegram component we are sending HTML encoded data
                .setHeader(TelegramConstants.TELEGRAM_PARSE_MODE, constant(TelegramParseMode.HTML))
                // Send the quote back to the same chat (CamelTelegramChatId header is implicitly used)
                .to("telegram:bots/{{token}}");
    }
}
