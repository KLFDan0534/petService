package com.pet.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.system.vo.RealNameReviewVO;

public interface RealNameReviewService {

    IPage<RealNameReviewVO> listPage(PageRequestDTO pageParam, Integer status, String keyword);

    void approve(Long userId, Long reviewerId, String remark);

    void reject(Long userId, Long reviewerId, String remark);
}
