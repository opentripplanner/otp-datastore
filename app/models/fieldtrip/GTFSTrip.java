package models.fieldtrip;
 
import com.google.gson.annotations.Expose;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import play.data.binding.As;
import play.db.jpa.Model;
 
/**
 *  A segment of a GTFS-defined trip which is part of a group itinerary
 */

@Entity
public class GTFSTrip extends Model {

    // The GroupItinerary that this trip belongs to
    @ManyToOne(optional=false)
    public GroupItinerary groupItinerary;

    @Temporal(TemporalType.TIME)
    @Expose
    @As("HH:mm:ss")
    public Date depart;

    @Temporal(TemporalType.TIME)
    @Expose
    @As("HH:mm:ss")
    public Date arrive;

    /* The hashed-based identifier for this trip */
    @Expose
    public String tripHash;

    // TODO: Do we still need this now that we use hashes?
    @Expose
    public String agencyAndId;

    @Expose
    public String routeName;

    @Expose
    public Integer fromStopIndex;

    @Expose
    public Integer toStopIndex;

    @Expose
    public Integer blockId;

    @Expose
    public String fromStopName;
    
    @Expose
    public String toStopName;
    
    @Expose
    public String headsign;

    @Expose
    public Integer capacity;
}