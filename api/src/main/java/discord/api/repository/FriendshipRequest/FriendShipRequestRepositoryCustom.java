package discord.api.repository.FriendshipRequest;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import discord.api.entity.connectionEntity.FriendshipRequest;
import discord.api.entity.dtos.user.NicknameNProfileIImgDto;
import discord.api.entity.dtos.user.QNicknameNProfileIImgDto;
import discord.api.entity.enums.FriendshipStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static discord.api.common.utils.QuerydslUtils.*;
import static discord.api.entity.QUser.*;
import static discord.api.entity.connectionEntity.QFriendshipRequest.friendshipRequest;

@Repository
@RequiredArgsConstructor
public class FriendShipRequestRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    public Page<NicknameNProfileIImgDto> getFriendInfoList(String email, Pageable pageable) {
        List<NicknameNProfileIImgDto> dtoList = queryFactory
                .select(new QNicknameNProfileIImgDto(user.nickname, user.profile_image))
                .from(user)
                .leftJoin(friendshipRequest)
                .on(friendshipRequest.sender.eq(user).or(friendshipRequest.receiver.eq(user)),
                        friendshipRequest.sender.email.eq(email).or(friendshipRequest.receiver.email.eq(email)))
                .where(
                        (friendshipRequest.status.eq(FriendshipStatus.ACCEPTED)),
                        (user.email.ne(email))
                )
                .orderBy(user.nickname.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(user.count())
                .from(user)
                .leftJoin(friendshipRequest).on(user.eq(friendshipRequest.sender).or(user.eq(friendshipRequest.receiver)))
                .where(
                        (friendshipRequest.status.eq(FriendshipStatus.ACCEPTED)),
                        (user.email.ne(email))
                );

        return PageableExecutionUtils.getPage(dtoList, pageable, countQuery::fetchOne);
    }

    public FriendshipRequest getBidirectionalFriendship(String senderEmail, String receiverEmail) {
        return queryFactory
                .selectFrom(friendshipRequest)
                .where(isEmailPairExist(senderEmail, receiverEmail))
                .fetchFirst();
    }

    public FriendshipRequest getFriendship(String senderEmail, String receiverEmail) {
        return queryFactory
                .selectFrom(friendshipRequest)
                .where(
                        senderEq(senderEmail),
                        receiverEq(receiverEmail)
                )
                .fetchFirst();
    }

    private BooleanBuilder senderEq(String email) {
        return nullSafeBuilder(() -> friendshipRequest.sender.email.eq(email));
    }

    private BooleanBuilder receiverEq(String email) {
        return nullSafeBuilder(() -> friendshipRequest.receiver.email.eq(email));
    }

    private BooleanBuilder isEmailPairExist(String senderEmail, String receiverEmail) {
        return (senderEq(senderEmail).and(receiverEq(receiverEmail)))
                .or(senderEq(receiverEmail).and(receiverEq(senderEmail)));
    }

    private BooleanBuilder statusEq(FriendshipStatus status) {
        return nullSafeBuilder(() -> friendshipRequest.status.eq(status));
    }
}
