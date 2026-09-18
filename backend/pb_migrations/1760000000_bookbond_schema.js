migrate((app) => {
  const users = app.findCollectionByNameOrId("users");
  users.listRule = '@request.auth.id != ""';
  users.viewRule = '@request.auth.id != ""';
  users.createRule = "";
  users.updateRule = 'id = @request.auth.id';
  users.deleteRule = 'id = @request.auth.id';
  users.authRule = "";
  users.passwordAuth.enabled = true;
  users.passwordAuth.identityFields = ["email"];
  users.fields.add(
    new TextField({ name: "displayName", required: true, min: 2, max: 60, presentable: true }),
    new NumberField({ name: "age", required: true, min: 18, max: 99, onlyInt: true }),
    new TextField({ name: "city", max: 100 }),
    new TextField({ name: "genres", max: 500 }),
    new TextField({ name: "books", max: 1000 }),
    new TextField({ name: "bio", max: 1500 }),
    new TextField({ name: "avatarEmoji", max: 16 })
  );
  app.save(users);

  const likes = new Collection({ type: "base", name: "likes" });
  likes.listRule = 'fromUser = @request.auth.id || toUser = @request.auth.id';
  likes.viewRule = 'fromUser = @request.auth.id || toUser = @request.auth.id';
  likes.createRule = 'fromUser = @request.auth.id && toUser != @request.auth.id';
  likes.updateRule = null;
  likes.deleteRule = 'fromUser = @request.auth.id';
  likes.fields.add(
    new RelationField({ name: "fromUser", required: true, maxSelect: 1, collectionId: users.id, cascadeDelete: true }),
    new RelationField({ name: "toUser", required: true, maxSelect: 1, collectionId: users.id, cascadeDelete: true })
  );
  likes.indexes = [
    "CREATE UNIQUE INDEX idx_likes_pair ON likes (fromUser, toUser)",
    "CREATE INDEX idx_likes_to ON likes (toUser)"
  ];
  app.save(likes);

  const messages = new Collection({ type: "base", name: "messages" });
  messages.listRule = 'sender = @request.auth.id || receiver = @request.auth.id';
  messages.viewRule = 'sender = @request.auth.id || receiver = @request.auth.id';
  messages.createRule = 'sender = @request.auth.id && receiver != @request.auth.id';
  messages.updateRule = 'receiver = @request.auth.id';
  messages.deleteRule = 'sender = @request.auth.id';
  messages.fields.add(
    new RelationField({ name: "sender", required: true, maxSelect: 1, collectionId: users.id, cascadeDelete: true }),
    new RelationField({ name: "receiver", required: true, maxSelect: 1, collectionId: users.id, cascadeDelete: true }),
    new TextField({ name: "body", required: true, min: 1, max: 4000 }),
    new BoolField({ name: "read" })
  );
  messages.indexes = [
    "CREATE INDEX idx_messages_sender_receiver ON messages (sender, receiver)",
    "CREATE INDEX idx_messages_receiver ON messages (receiver)"
  ];
  app.save(messages);

  const posts = new Collection({ type: "base", name: "posts" });
  posts.listRule = '@request.auth.id != ""';
  posts.viewRule = '@request.auth.id != ""';
  posts.createRule = 'author = @request.auth.id';
  posts.updateRule = 'author = @request.auth.id';
  posts.deleteRule = 'author = @request.auth.id';
  posts.fields.add(
    new RelationField({ name: "author", required: true, maxSelect: 1, collectionId: users.id, cascadeDelete: true }),
    new TextField({ name: "kind", required: true, max: 40 }),
    new TextField({ name: "book", max: 300 }),
    new TextField({ name: "text", required: true, min: 1, max: 3000 })
  );
  posts.indexes = [
    "CREATE INDEX idx_posts_author ON posts (author)"
  ];
  app.save(posts);

  const settings = app.settings();
  settings.meta.appName = "BookBond";
  settings.logs.maxDays = 7;
  settings.logs.logIP = false;
  app.save(settings);
}, (app) => {
  for (const name of ["posts", "messages", "likes"]) {
    try { app.delete(app.findCollectionByNameOrId(name)); } catch (_) {}
  }
  try {
    const users = app.findCollectionByNameOrId("users");
    for (const field of ["displayName","age","city","genres","books","bio","avatarEmoji"]) {
      users.fields.removeByName(field);
    }
    app.save(users);
  } catch (_) {}
});
