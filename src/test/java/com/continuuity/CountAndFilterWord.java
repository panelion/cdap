package com.continuuity;

import com.continuuity.api.Application;
import com.continuuity.api.ApplicationSpecification;
import com.continuuity.api.annotation.UseDataSet;
import com.continuuity.api.data.OperationException;
import com.continuuity.api.data.dataset.KeyValueTable;
import com.continuuity.api.data.stream.Stream;
import com.continuuity.api.flow.Flow;
import com.continuuity.api.flow.FlowSpecification;
import com.continuuity.api.flow.flowlet.AbstractFlowlet;
import com.continuuity.api.flow.flowlet.OutputEmitter;
import com.continuuity.api.flow.flowlet.StreamEvent;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class CountAndFilterWord implements Application {
  /**
   * Configures the {@link com.continuuity.api.Application} by returning an {@link com.continuuity.api.ApplicationSpecification}
   *
   * @return An instance of {@code ApplicationSpecification}.
   */
  @Override
  public ApplicationSpecification configure() {
    return ApplicationSpecification.Builder.with()
      .setName("CountAndFilterWordApp")
      .setDescription("")
      .withStreams()
      .add(new Stream("text"))
      .withDataSets()
      .add(new KeyValueTable(Common.counterTableName))
      .withFlows()
        .add(new CountAndFilterWordFlow())
      .noProcedure()
      .noBatch()
      .build();
  }

  private static class CountAndFilterWordFlow implements Flow {
    @Override
    public FlowSpecification configure() {
      return FlowSpecification.Builder.with()
        .setName("CountAndFilterWordFlow")
        .setDescription("Flow for counting words")
        .withFlowlets()
        .add("source", new StreamSource(), 1)
        .add("split-words", new Tokenizer(), 1)
        .add("upper-filter", new UpperCaseFilter(), 1)
        .add("count-all", new CountByField(), 1)
        .add("count-upper", new CountByField(), 1)
        .connect()
        .fromStream("text").to("source")
        .from("source").to("split-words")
        .from("split-words").to("count-all")
        .from("split-words").to("upper-filter")
        .from("upper-filter").to("count-upper")
        .build();
    }
  }

  private static class StreamSource extends AbstractFlowlet {
    private OutputEmitter<Map<String,String>> output;

    public StreamSource() {
      super("source");
    }

    public void process(StreamEvent event) {
      if (Common.debug) {
        System.out.println(this.getClass().getSimpleName() + ": Received event " + event);
      }

      Map<String, String> headers = event.getHeaders();
      String title = headers.get("title");
      byte[] body = event.getBody().array();
      String text = body == null ? "" :new String(body);

      Map<String,String> tuple = new HashMap<String,String>();
      tuple.put("title", title);
      tuple.put("text", text);

      if (Common.debug) {
        System.out.println(this.getClass().getSimpleName() + ": Emitting tuple " + output);
      }
      output.emit(tuple);
    }
  }

  private static class Tokenizer extends AbstractFlowlet {
    private OutputEmitter<Map<String,String>> output;

    public Tokenizer() {
      super("split-words");
    }

    public void process(Map<String, String> map) {
      final String[] fields = { "title", "text" };

      if (Common.debug) {
        System.out.println(this.getClass().getSimpleName() + ": Received tuple " + map);
      }
      for (String field : fields) {
        tokenize((String)map.get(field), field);
      }
    }

    void tokenize(String str, String field) {
      if (str == null) {
        return;
      }
      final String delimiters = "[ .-]";
      String[] tokens = str.split(delimiters);

      for (String token : tokens) {
        Map<String,String> tuple = new HashMap<String,String>();
        tuple.put("field", field);
        tuple.put("word", token);

        if (Common.debug) {
          System.out.println(this.getClass().getSimpleName() + ": Emitting tuple " + output);
        }
        output.emit(tuple);
      }
    }
  }

  private static class UpperCaseFilter extends AbstractFlowlet {
    private OutputEmitter<Map<String,String>> output;

    public UpperCaseFilter() {
      super("upper-filter");
    }

    public void process(Map<String, String> tupleIn) {
      if (Common.debug) {
        System.out.println(this.getClass().getSimpleName() + ": Received tuple " + tupleIn);
      }
      String word = tupleIn.get("word");
      if (word == null) {
        return;
      }
      // filter words that are not upper-cased
      if (!Character.isUpperCase(word.charAt(0))) {
        return;
      }

      Map<String,String> tupleOut = new HashMap<String,String>();
      tupleOut.put("word", word);

      if (Common.debug) {
        System.out.println(this.getClass().getSimpleName() + ": Emitting tuple " + tupleOut);
      }
      output.emit(tupleOut);
    }
  }


  private static class CountByField extends AbstractFlowlet {

    @UseDataSet(Common.counterTableName)
    KeyValueTable counters;

    public CountByField() {
      super("countByField");
    }

    public void process(Map<String, String> tupleIn) {
      if (Common.debug) {
        System.out.println(this.getClass().getSimpleName() + ": Received tuple " + tupleIn);
      }

      String token = tupleIn.get("word");
      if (token == null) return;
      String field = tupleIn.get("field");
      if (field != null) token = field + ":" + token;

      if (Common.debug) {
        System.out.println(this.getClass().getSimpleName() + ": Incrementing for " + token);
      }
      try {
        this.counters.increment(token.getBytes(), 1L);
      } catch (OperationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class Common {
    static boolean verbose = false;
    static boolean debug = true;
    final static String counterTableName = "fieldcount";
  }

}
