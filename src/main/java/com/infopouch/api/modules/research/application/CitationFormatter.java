package com.infopouch.api.modules.research.application;

import com.infopouch.api.common.exception.ValidationException;
import com.infopouch.api.modules.research.presentation.dto.ResearchResponse;
import java.util.List;

public class CitationFormatter {

  private CitationFormatter() {}

  public static String format(ResearchResponse paper, String format) {
    if (format == null) {
      throw new ValidationException("Citation format is required.");
    }

    return switch (format.toUpperCase()) {
      case "APA" -> apa(paper);
      case "MLA" -> mla(paper);
      default -> throw new ValidationException("Unsupported citation format: " + format);
    };
  }

  private static String apa(ResearchResponse paper) {
    return apaAuthors(paper.authors())
        + " ("
        + paper.publicationYear()
        + "). "
        + paper.title()
        + ". "
        + paper.institution()
        + ".";
  }

  private static String mla(ResearchResponse paper) {
    return mlaAuthors(paper.authors())
        + " \""
        + paper.title()
        + ".\" "
        + paper.institution()
        + ", "
        + paper.publicationYear()
        + ".";
  }

  private static String apaAuthors(List<String> authors) {
    if (authors.size() == 1) {
      return authors.get(0);
    }
    return String.join(", ", authors.subList(0, authors.size() - 1))
        + ", & "
        + authors.get(authors.size() - 1);
  }

  private static String mlaAuthors(List<String> authors) {
    if (authors.size() == 1) {
      return authors.get(0) + ".";
    }
    return authors.get(0) + ", et al.";
  }
}
