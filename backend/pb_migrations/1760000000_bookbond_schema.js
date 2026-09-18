migrate((app) => {
  const users = new Collection({
    type: "auth",
    name: "users",
    listRule: '@request.auth.id != ""',
    viewRule: '@request.auth.id != ""',
    createRule: "",
    updateRule: 'id = @request.auth.id',
    deleteRule: 'id = @request.auth.id',
    authRule: "",
    passwordAuth: { enabled: true, identityFields: ["email"] },
    fields: [
      { name: "displayName", type: "text", required: true, min: 2, max: 60, presentable: true },
      { name: "age", type: "number", required: true, min: 18, max: 99, onlyInt: true },
      { name: "city", type: "text", max: 100 },
      { name: "genres", type: "text", max: 500 },
      { name: "books", type: "text", max: 1000 },
      { name: "bio", type: "text", max: 1500 },
      { name: "avatar", type: "text", max: 16 },
    ],
    indexes: [
      "CREATE INDEX idx_users_city ON users (city)",
      "CREATE INDEX idx_users_updated ON users (updated)",
    ],
  });
  app.save(users);

  const likes = new Collection({
    type: "base", name: "likes",
    listRule: 'fromUser = @request.auth.id || toUser = @request.auth.id',
    viewRule: 'fromUser = @request.auth.id || toUser = @request.auth.id',
    createRule: 'fromUser = @request.auth.id && toUser != @request.auth.id',
    updateRule: null,
    deleteRule: 'fromUser = @request.auth.id',
    fields: [
      { name: "fromUser", type: "relation", required: true, maxSelect: 1, collectionId: users.id, cascadeDelete: true },
      { name: "toUser", type: "relation", required: true, maxSelect: 1, collectionId: users.id, cascadeDelete: true },
    ],
    indexes: [
      "CREATE UNIQUE INDEX idx_likes_pair ON likes (fromUser, toUser)",
      "CREATE INDEX idx_likes_to ON likes (toUser)",
    ],
  });
  app.save(likes);

  const messages = new Collection({
    type: "base", name: "messages",
    listRule: 'sender = @request.auth.id || receiver = @request.auth.id',
    viewRule: 'sender = @request.auth.id || receiver = @request.auth.id',
    createRule: 'sender = @request.auth.id && receiver != @request.auth.id',
    updateRule: 'receiver = @request.auth.id',
    deleteRule: 'sender = @request.auth.id',
    fields: [
      { name: "sender", type: "relation", required: true, maxSelect: 1, collectionId: users.id, cascadeDelete: true },
      { name: "receiver", type: "relation", required: true, maxSelect: 1, collectionId: users.id, cascadeDelete: true },
      { name: "body", type: "text", required: true, min: 1, max: 4000 },
      { name: "read", type: "bool" },
    ],
    indexes: [
      "CREATE INDEX idx_messages_sender_receiver_created ON messages (sender, receiver, created)",
      "CREATE INDEX idx_messages_receiver_created ON messages (receiver, created)",
    ],
  });
  app.save(messages);

  const posts = new Collection({
    type: "base", name: "posts",
    listRule: '@request.auth.id != ""',
    viewRule: '@request.auth.id != ""',
    createRule: 'author = @request.auth.id',
    updateRule: 'author = @request.auth.id',
    deleteRule: 'author = @request.auth.id',
    fields: [
      { name: "author", type: "relation", required: true, maxSelect: 1, collectionId: users.id, cascadeDelete: true },
      { name: "kind", type: "text", required: true, max: 40 },
      { name: "book", type: "text", max: 300 },
      { name: "text", type: "text", required: true, min: 1, max: 3000 },
    ],
    indexes: [
      "CREATE INDEX idx_posts_created ON posts (created)",
      "CREATE INDEX idx_posts_author ON posts (author)",
    ],
  });
  app.save(posts);

  const settings = app.settings();
  settings.meta.appName = "BookBond";
  settings.logs.maxDays = 7;
  settings.logs.logIP = false;
  app.save(settings);
}, (app) => {
  for (const name of ["posts", "messages", "likes", "users"]) {
    try { app.delete(app.findCollectionByNameOrId(name)); } catch (_) {}
  }
});
