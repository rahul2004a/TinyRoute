package com.tinyroute.repository.jpa;

import com.tinyroute.model.*;
import com.tinyroute.repository.ClickEventRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface JpaClickEventRepository
        extends Repository<ClickEvent, UUID>, ClickEventRepository {
    @Override
    @org.springframework.transaction.annotation.Transactional(timeout = 5)
    default void record(String code, Instant clickedAt, ClickMetadata metadata) {
        insertAndIncrement(
                UUID.randomUUID(),
                code,
                clickedAt,
                metadata.referrer(),
                metadata.device(),
                metadata.operatingSystem(),
                metadata.browser(),
                metadata.country(),
                metadata.city());
    }

    @Modifying
    @Query(
            value =
                    """
        with recorded as (
            insert into click_events(id,link_id,clicked_at,referrer_domain,device,operating_system,browser,country,city)
            select :id,l.id,:clickedAt,:referrer,:device,:os,:browser,:country,:city
            from links l join users u on u.id=l.owner_id
            where l.code=:code and u.deleted_at is null
            returning link_id
        )
        update links set click_count=click_count+1 from recorded where links.id=recorded.link_id
        """,
            nativeQuery = true)
    int insertAndIncrement(
            @Param("id") UUID id,
            @Param("code") String code,
            @Param("clickedAt") Instant clickedAt,
            @Param("referrer") String referrer,
            @Param("device") String device,
            @Param("os") String os,
            @Param("browser") String browser,
            @Param("country") String country,
            @Param("city") String city);

    @Override
    @Query(
            value =
                    """
        select coalesce(sum(click_count),0) from links
        where owner_id=:ownerId and deleted_at is null and status<>'DELETED'
        and (cast(:linkId as uuid) is null or id=cast(:linkId as uuid))
        """,
            nativeQuery = true)
    long lifetime(@Param("ownerId") UUID ownerId, @Param("linkId") UUID linkId);

    @Override
    @Query(
            value =
                    """
        select cast(e.clicked_at at time zone 'UTC' as date) as date,count(*) as count
        from click_events e join links l on l.id=e.link_id
        where l.owner_id=:ownerId and l.deleted_at is null and l.status<>'DELETED'
        and (cast(:linkId as uuid) is null or l.id=cast(:linkId as uuid))
        and e.clicked_at>=:from and e.clicked_at<:to
        group by date order by date
        """,
            nativeQuery = true)
    List<DailyCount> daily(
            @Param("ownerId") UUID ownerId,
            @Param("linkId") UUID linkId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Override
    @Query(
            value =
                    """
        with values_in_range as (
            select case :dimension
                when 'referrers' then e.referrer_domain when 'devices' then e.device
                when 'operatingSystems' then e.operating_system when 'browsers' then e.browser
                when 'countries' then e.country when 'cities' then e.city end as label
            from click_events e join links l on l.id=e.link_id
            where l.owner_id=:ownerId and l.deleted_at is null and l.status<>'DELETED'
            and (cast(:linkId as uuid) is null or l.id=cast(:linkId as uuid))
            and e.clicked_at>=:from and e.clicked_at<:to
        ), grouped as (
            select case when label is null or label='Unknown' then 'UNKNOWN'
                        when label='' then 'DIRECT_OR_UNKNOWN' else 'VALUE' end as kind,
                   coalesce(nullif(label,''),'Unknown') as label,count(*) as count
            from values_in_range group by 1,2
        ), ranked as (
            select *,row_number() over(partition by kind order by count desc,label collate "C") as position from grouped
        )
        ,bucketed as (select case when kind='VALUE' and position>20 and :dimension<>'countries' then 'OTHER' else kind end as kind,
               case when kind='VALUE' and position>20 and :dimension<>'countries' then 'Other'
                    when kind='DIRECT_OR_UNKNOWN' then 'Direct / unknown' else label end as label,
               sum(count) as count
        from ranked group by 1,2) select kind,label,count from bucketed order by count desc,label collate "C"
        """,
            nativeQuery = true)
    List<BucketCount> breakdown(
            @Param("ownerId") UUID ownerId,
            @Param("linkId") UUID linkId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("dimension") String dimension);
}
