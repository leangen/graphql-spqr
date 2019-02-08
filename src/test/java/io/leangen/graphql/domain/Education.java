package io.leangen.graphql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.graphql.Query;

/**
 * Created by bojan.tomic on 5/28/16.
 */
public class Education {

    public enum Tier {
        TOP, BOTTOM
    }

    private String schoolName;
    private Integer startYear;
    private Integer endYear;
    private Tier tier;

    @JsonCreator
    public Education(@JsonProperty("schoolName") String schoolName, 
                     @JsonProperty("startYear") int startYear, 
                     @JsonProperty("endYear") int endYear) {
        this.schoolName = schoolName;
        this.startYear = startYear;
        this.endYear = endYear;
    }

    @Query(value = "schoolName", description = "The school where education was obtained")
    public String getSchoolName() {
        return schoolName;
    }

    @Query(value = "startYear", description = "The starting year of education")
    public Integer getStartYear() {
        return startYear;
    }


    @Query(value = "endYear", description = "The final year of education")
    public Integer getEndYear() {
        return endYear;
    }

    @Query(value = "tier", description = "The school tier")
    public Tier getTier() {
        return tier;
    }
}
