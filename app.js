const express = require("express");
const session = require("express-session");
const app = express();
const bodyParser = require("body-parser");
const favicon = require("serve-favicon");
const path = require("path");
const router = require("./router");
const port = process.env.PORT || 3000;

require("dotenv").config();

app.use(
  session({ secret: "catisgodmeow", resave: true, saveUninitialized: true })
);

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

app.use(express.static("public"));
app.use("/uploads", express.static(__dirname + "/uploads"));
app.set("view engine", "ejs");
app.use(favicon(path.join("public", "images", "favicon.ico")));

app.use("/", router);

app.listen(port, () => {
  console.log("Server is running on PORT", port);
});
