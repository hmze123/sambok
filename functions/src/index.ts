import * as admin from "firebase-admin";
import {onDocumentCreated, onDocumentUpdated} from "firebase-functions/v2/firestore";

admin.initializeApp();

const db = admin.firestore();
const fcm = admin.messaging();

/**
 * إشعار المتابعة الجديدة
 */
export const onNewFollower = onDocumentCreated("users/{userId}/followers/{followerId}", async (event) => {
  const context = event.params;
  const userId = context.userId;
  const followerId = context.followerId;

  if (userId === followerId) return;

  const followerDoc = await db.collection("users").doc(followerId).get();
  const followerData = followerDoc.data();
  const userDoc = await db.collection("users").doc(userId).get();
  const fcmToken = userDoc.data()?.fcmToken;

  if (fcmToken && followerData) {
    const payload = {
      notification: {
        title: "New Follower!",
        body: `${followerData.displayName || followerData.username} started following you.`,
        icon: "ic_notification",
      },
      // --- التعديل هنا: إضافة حمولة البيانات ---
      data: {
        click_action: "PROFILE",
        user_id: followerId,
      },
    };
    return fcm.sendToDevice(fcmToken, payload);
  }
  return;
});

/**
 * إشعار التفاعل الجديد
 */
export const onNewReaction = onDocumentUpdated("posts/{postId}", async (event) => {
  if (!event.data) return;

  const context = event.params;
  const postId = context.postId;
  const beforeData = event.data.before.data();
  const afterData = event.data.after.data();

  const beforeReactions = beforeData.reactions || {};
  const afterReactions = afterData.reactions || {};
  const reactingUserId = Object.keys(afterReactions).find(
    (key) => beforeReactions[key] !== afterReactions[key]
  );

  if (!reactingUserId) return;
  
  const postAuthorId = afterData.authorId;
  if (postAuthorId === reactingUserId) return;

  const reactorDoc = await db.collection("users").doc(reactingUserId).get();
  const reactorData = reactorDoc.data();
  const authorDoc = await db.collection("users").doc(postAuthorId).get();
  const authorFcmToken = authorDoc.data()?.fcmToken;

  if (authorFcmToken && reactorData) {
    const emoji = afterReactions[reactingUserId];
    const payload = {
      notification: {
        title: "New Reaction!",
        body: `${reactorData.displayName || reactorData.username} reacted ${emoji} to your post.`,
        icon: "ic_notification",
      },
      // --- التعديل هنا: إضافة حمولة البيانات ---
      data: {
        click_action: "POST",
        post_id: postId,
      },
    };
    return fcm.sendToDevice(authorFcmToken, payload);
  }
  return;
});


/**
 * إشعار الرسالة الجديدة
 */
export const onNewChatMessage = onDocumentCreated("chats/{chatId}/messages/{messageId}", async (event) => {
    if (!event.data) return;
    const context = event.params;
    const chatId = context.chatId;
    const messageData = event.data.data();
    const senderId = messageData.senderId;
    const senderName = messageData.senderName || "Someone";

    let messageBody = "Sent you a message.";
    if (messageData.type === "text" && messageData.content) {
        messageBody = messageData.content;
    } else if (messageData.type === "image") {
        messageBody = "Sent you an image.";
    } else if (messageData.type === "video") {
        messageBody = "Sent you a video.";
    }

    const chatDoc = await db.collection("chats").doc(chatId).get();
    const participants = chatDoc.data()?.participants;
    if (!participants) return;

    const recipientIds = participants.filter((id: string) => id !== senderId);

    const tokens: string[] = [];
    for (const recipientId of recipientIds) {
        const userDoc = await db.collection("users").doc(recipientId).get();
        const fcmToken = userDoc.data()?.fcmToken;
        if (fcmToken) {
            tokens.push(fcmToken);
        }
    }

    if (tokens.length > 0) {
        const payload = {
            notification: {
                title: `New Message from ${senderName}`,
                body: messageBody,
                icon: "ic_notification",
            },
            // --- التعديل هنا: إضافة حمولة البيانات ---
            data: {
                click_action: "CHAT",
                chat_id: chatId,
            },
        };
        return fcm.sendToDevice(tokens, payload);
    }
    return;
});