package org.edupoll.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.edupoll.exception.NotExistUserException;
import org.edupoll.model.dto.FeedWrapper;
import org.edupoll.model.dto.request.CreateFeedRequest;
import org.edupoll.model.entity.Feed;
import org.edupoll.model.entity.FeedAttach;
import org.edupoll.repository.FeedAttachRepository;
import org.edupoll.repository.FeedRepository;
import org.edupoll.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

	private final UserRepository userRepository;
	private final FeedRepository feedRepository;
	private final FeedAttachRepository feedAttachRepository;
	@Value("${upload.basedir}")
	private String uploadBaseDir;
	@Value("${upload.server}")
	private String uploadServer;

	@Transactional
	public boolean create(String principal, CreateFeedRequest request)
			throws NotExistUserException, IllegalStateException, IOException {
		// 1. Feed Entity 생성 save
		var user = userRepository.findByEmail(principal).orElseThrow(() -> new NotExistUserException());

		var feed = new Feed();
		feed.setDescription(request.getDescription());
		feed.setViewCount(0L);
		feed.setWriter(user);
		var saved = feedRepository.save(feed);

		log.info("attaches is exist ? {}", request.getAttaches() != null);

		if (request.getAttaches() != null) { // 파일이 넘어왔다면
			File uploadDirectory = new File(uploadBaseDir + "/feed/" + saved.getId());
			uploadDirectory.mkdirs();

			for (MultipartFile multi : request.getAttaches()) { // 하나씩 반복문 돌면서
				// 어디다가 file 옮겨둘껀지 File 객체로 정의하고
				String fileName = String.valueOf(System.currentTimeMillis());
				String extension = multi.getOriginalFilename().split("\\.")[1];
				File dest = new File(uploadDirectory, fileName + "." + extension);

				multi.transferTo(dest); // 옮기는걸 진행

				// 업로드가 끝나면 DB에 기록
				FeedAttach feedAttach = new FeedAttach();
				feedAttach.setType(multi.getContentType());
				// 업로드를 한 곳이 어디냐에 따라서 결정이 되는 값
				feedAttach.setMediaUrl(
						uploadServer + "/resource/feed/" + saved.getId() + "/" + fileName + "." + extension);
				feedAttach.setFeed(saved);
				feedAttachRepository.save(feedAttach);
			}
		}
		return true;
	}

	public Long size() {
		return feedRepository.count();
	}

	public List<FeedWrapper> allItems(int page) {
		// 10개만 보내게
		List<Feed> entityList = feedRepository.findAll(PageRequest.of(page - 1, 10, Sort.by("id").descending()))
				.toList();

		return entityList.stream().map(e -> new FeedWrapper(e)).toList();
	}

}
