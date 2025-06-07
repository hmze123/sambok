import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();
const fcm = admin.messaging();

/**
 * إشعار المتابعة الجديدة
 * يتم تفعيله عند إضافة مستخدم جديد إلى قائمة "المتابعين" لمستخدم آخر.
 */
export const onNewFollower = functions.firestore
  .document("users/{userId}/followers/{followerId}")
  .onCreate(async (snapshot, context) => {
    const userId = context.params.userId; // الشخص الذي تمت متابعته
    const followerId = context.params.followerId; // الشخص الذي قام بالمتابعة

    if (userId === followerId) {
      console.log("User followed themselves, no notification sent.");
      return null;
    }

    // جلب بيانات المتابع
    const followerDoc = await db.collection("users").doc(followerId).get();
    const followerData = followerDoc.data();

    // جلب توكن الإشعارات الخاص بالمستخدم الذي تمت متابعته
    const userDoc = await db.collection("users").doc(userId).get();
    const fcmToken = userDoc.data()?.fcmToken;

    if (fcmToken && followerData) {
      const payload = {
        notification: {
          title: "New Follower!",
          body: `${followerData.displayName || followerData.username} started following you.`,
          icon: "ic_notification",
          click_action: `PROFILE/${followerId}`,
        },
      };
      console.log(`Sending new follower notification to ${userId}`);
      return fcm.sendToDevice(fcmToken, payload);
    }
    return null;
  });

/**
 * إشعار الإعجاب/التفاعل الجديد
 * يتم تفعيله عند إنشاء أو تعديل تفاعل على منشور.
 */
export const onNewReaction = functions.firestore
  .document("posts/{postId}")
  .onUpdate(async (change, context) => {
    const postId = context.params.postId;
    const beforeData = change.before.data();
    const afterData = change.after.data();

    // مقارنة خريطة التفاعلات قبل وبعد
    const beforeReactions = beforeData.reactions || {};
    const afterReactions = afterData.reactions || {};

    // البحث عن المستخدم الذي قام بالتفاعل
    const reactingUserId = Object.keys(afterReactions).find(
      (key) => beforeReactions[key] !== afterReactions[key]
    );

    if (!reactingUserId) {
      console.log("No new reaction found.");
      return null;
    }
    
    const postAuthorId = afterData.authorId;

    if (postAuthorId === reactingUserId) {
        console.log("User reacted to their own post, no notification sent.");
        return null;
    }

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
                click_action: `POST/${postId}`,
            },
        };
        console.log(`Sending reaction notification to ${postAuthorId}`);
        return fcm.sendToDevice(authorFcmToken, payload);
    }
    return null;
  });

/**
 * إشعار الرسالة الجديدة
 * يتم تفعيله عند إضافة رسالة جديدة في محادثة.
 */
export const onNewChatMessage = functions.firestore
  .document("chats/{chatId}/messages/{messageId}")
  .onCreate(async (snapshot, context) => {
    const chatId = context.params.chatId;
    const messageData = snapshot.data();
    const senderId = messageData.senderId;
    const senderName = messageData.senderName || "Someone";
    
    // تحديد نص الرسالة بناءً على نوعها
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

    if (!participants) return null;

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
          click_action: `CHAT/${chatId}`,
        },
      };
      console.log(`Sending message notification to ${tokens.length} users.`);
      return fcm.sendToDevice(tokens, payload);
    }
    return null;
  });