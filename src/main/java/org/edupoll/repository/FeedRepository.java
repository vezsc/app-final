package org.edupoll.repository;

import org.edupoll.model.entity.Feed;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedRepository  extends JpaRepository<Feed, Long>{

}
