package com.infopouch.api.modules.research.infrastructure;

import com.infopouch.api.modules.research.domain.ResearchKeyword;
import com.infopouch.api.modules.research.domain.ResearchPaper;
import com.infopouch.api.modules.research.domain.ResearchStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class ResearchSpecifications {

  private ResearchSpecifications() {}

  public static Specification<ResearchPaper> search(
      ResearchStatus status,
      String query,
      String researchField,
      String institution,
      Integer year,
      String country) {
    return (root, criteriaQuery, builder) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(builder.equal(root.get("status"), status));

      if (query != null && !query.isBlank()) {
        String like = "%" + query.toLowerCase() + "%";
        Join<ResearchPaper, ResearchKeyword> keywordJoin =
            root.join("keywords", jakarta.persistence.criteria.JoinType.LEFT);
        criteriaQuery.distinct(true);
        predicates.add(
            builder.or(
                builder.like(builder.lower(root.get("title")), like),
                builder.like(builder.lower(root.get("abstractText")), like),
                builder.like(builder.lower(keywordJoin.get("keyword")), like)));
      }

      if (researchField != null && !researchField.isBlank()) {
        predicates.add(
            builder.equal(builder.lower(root.get("researchField")), researchField.toLowerCase()));
      }

      if (institution != null && !institution.isBlank()) {
        predicates.add(
            builder.equal(builder.lower(root.get("institution")), institution.toLowerCase()));
      }

      if (year != null) {
        predicates.add(builder.equal(root.get("publicationYear"), year));
      }

      if (country != null && !country.isBlank()) {
        predicates.add(
            builder.equal(builder.lower(root.get("countryOfStudy")), country.toLowerCase()));
      }

      return builder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
