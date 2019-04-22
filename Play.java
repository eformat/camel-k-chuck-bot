import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

public class Play extends RouteBuilder {

    public void configure() throws Exception {

        // @formatter:off
        from("telegram:bots/{{token}}")
                //.filter(simple("${body.from.id} == 768785980"))
                .convertBodyTo(String.class)
                .choice()
                    .when(simple("${body.toLowerCase()} contains 'chuck'"))
                    .to("http4://api.icndb.com/jokes/random")
                    .unmarshal().json(JsonLibrary.Jackson)
                    .transform(simple("${body[value][joke]}"))
                    .to("telegram:bots/{{token}}")
                    .log("${body}")
                .otherwise()
                    .log("Discarded: ${body}");
        //@formatter:on
    }
}
