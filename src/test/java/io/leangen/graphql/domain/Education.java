package io.leangen.graphql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.leangen.graphql.annotations.Query;

/**
 * Created by bojan.tomic on 5/28/16.
 */
public class Education {

    public enum Tier {
        TOP, BOTTOM
    }

    @Query(value = "schoolName", description = "The school where education was obtained")
    public String schoolName;
    @Query(value = "startYear", description = "The starting year of education")
    public Integer startYear;
    @Query(value = "endYear", description = "The final year of education")
    public Integer endYear;
    @Query(value = "tier", description = "The school tier")
    public Tier tier;

    @JsonCreator
    public Education(@JsonProperty("schoolName") String schoolName, 
                     @JsonProperty("startYear") int startYear, 
                     @JsonProperty("endYear") int endYear) {
        this.schoolName = schoolName;
        this.startYear = startYear;
        this.endYear = endYear;
    }


    public String getSchoolName() {
        return schoolName;
    }


    public Integer getStartYear() {
        return startYear;
    }


    public Integer getEndYear() {
        return endYear;
    }
}
