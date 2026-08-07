const {onCall, HttpsError} =
  require("firebase-functions/v2/https");

const {initializeApp} =
  require("firebase-admin/app");

const {getAuth} =
  require("firebase-admin/auth");

const {getFirestore} =
  require("firebase-admin/firestore");

initializeApp();

exports.resetUserPassword = onCall(
  async (request) => {
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "Please sign in."
      );
    }

    const managerUid =
      request.auth.uid;

    const targetUid =
      request.data?.targetUid;

    if (
      typeof targetUid !== "string" ||
      targetUid.trim() === ""
    ) {
      throw new HttpsError(
        "invalid-argument",
        "A target user ID is required."
      );
    }

    if (targetUid === managerUid) {
      throw new HttpsError(
        "failed-precondition",
        "Managers cannot reset their own password here."
      );
    }

    const managerDocument =
      await getFirestore()
        .collection("users")
        .doc(managerUid)
        .get();

    const managerRole =
      managerDocument
        .get("role")
        ?.toString()
        .toUpperCase();

    if (managerRole !== "MANAGER") {
      throw new HttpsError(
        "permission-denied",
        "Only managers can reset passwords."
      );
    }

    await getAuth().updateUser(
      targetUid,
      {
        password: "12345678"
      }
    );

    return {
      success: true
    };
  }
);
