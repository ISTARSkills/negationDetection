package ai.salesken.negation;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.TypedDependency;

@WebServlet(urlPatterns = { "/api" }, loadOnStartup = 1)
public class NegationService extends HttpServlet {
	private static final long serialVersionUID = -5074391661245825712L;
	private static final Logger log = LogManager.getLogger(NegationService.class);
	private MaxentTagger tagger;
	private DependencyParser parser;

	@Override
	public void init(ServletConfig config) throws ServletException {
		long start = System.currentTimeMillis();
		log.info("Please wait while the model is initialized");
		String modelPath = DependencyParser.DEFAULT_MODEL;
		String taggerPath = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";
		tagger = new MaxentTagger(taggerPath);
		parser = DependencyParser.loadFromModelFile(modelPath);
		log.info("Model was successfully initialized in " + (System.currentTimeMillis() - start));
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		JsonObject jsonObjectR = new JsonObject();
		int negationCountTotal = 0;
		try {
			String text = req.getParameter("text");
			JsonArray array = new JsonArray();
			DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
			for (List<HasWord> sentence : tokenizer) {
				int negationCount = 0;
				JsonObject jsonObject = new JsonObject();
				List<TaggedWord> tagged = tagger.tagSentence(sentence);
				GrammaticalStructure gs = parser.predict(tagged);
				for (TypedDependency dependency : gs.allTypedDependencies()) {
					if (dependency.reln().getLongName().trim().equalsIgnoreCase("negation modifier"))
						negationCount += 1;
					// log.info(dependency.reln().getLongName());
					// log.info(dependency.toString());
				}
				jsonObject.addProperty("text", sentence.toString());
				jsonObject.addProperty("negation_count", negationCount);
				array.add(jsonObject);
				negationCountTotal += negationCount;
				log.info(negationCount + negationCountTotal + text + gs);
			}
			jsonObjectR.addProperty("success", true);
			if (negationCountTotal % 2 == 0)
				jsonObjectR.addProperty("result", "positive");
			else
				jsonObjectR.addProperty("result", "negative");
			jsonObjectR.addProperty("negation_count", negationCountTotal);
			jsonObjectR.add("texts", array);
		} catch (Exception e) {
			jsonObjectR.addProperty("success", false);
			jsonObjectR.addProperty("message", e.getMessage());
		}
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setContentType("application/json");
		resp.getWriter().append(jsonObjectR.toString());
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}

}
