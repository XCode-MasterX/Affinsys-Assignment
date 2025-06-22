package com.restapi.taskManagement;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.mindrot.jbcrypt.BCrypt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

@RestController
public class Controller {
    private ActiveSessionHolder session = ActiveSessionHolder.getInstance();
    private Database db = Database.getInstance();
    private Gson gson = new Gson();
    private String API_KEY = null;

    static final String AUTH = "authorization";

    //Functionality Status: Works
    public Response checkAuthentication(HashMap<String, String> header, String message) {
        if(!header.containsKey(AUTH))
            return new Response().setStatus(401)
            .setError("No Authorization token found")
            .setMessage(message);

        if(session.isSessionActive(header.get(AUTH))) return null;

        return new Response()
        .setStatus(401)
        .setError("No active session")
        .setMessage("No active login with this authorization.");
    }

    // FUNCTION NO. 1
    // Functionality Status: Works
    @PostMapping("/register")
    public Response createUser(@RequestBody HashMap<String, String> body) {
        if(!body.containsKey("username") || !body.containsKey("password"))
            return new Response().setStatus(400)
            .setError("Invalid request body")
            .setMessage("The request body is missing username or password. Both are needed for registering.");

        Account acc = new Account()
                .setUsername(body.get("username"))
                .setPassword(body.get("password"));

        String x[] = db.getQuery("select count(*) from Users where username = ?", acc.getUsername());
        
        if(x != null) {
            int res = Integer.parseInt(gson.fromJson(x[0], HashMap.class).get("count(*)").toString());
            if(res != 0)
                return new Response()
                .setStatus(409)
                .setError("Conflicting username")
                .setMessage("Your username is already under use by someone else.");
        }

        int updated = db.updateQuery("insert into Users values(?, ?, ?, ?)", acc.getAuthToken(), acc.getUsername(), acc.getHashedPassword(), 0);
        
        if(updated > 0)
            return new Response()
            .setStatus(201)
            .setMessage("User successfully created.");
        else
            return new Response()
            .setStatus(500)
            .setMessage("There was an error with adding the user to the Database. Try again later.");
    }

    // FUNCTION NO. 2
    // Functionality Status: Works
    @PostMapping("/fund")
    public Response depositAmount(@RequestHeader HashMap<String, String> header, @RequestBody HashMap<String, Object> body) {
        Response res = checkAuthentication(header, "You need to provide an authorization token before being able deposit money.");       
        if(res != null) return res;

        if(!body.containsKey("amt"))
            return new Response()
            .setStatus(400)
            .setError("Information missing from request body")
            .setMessage("You need to provide the amount that you want to deposit to your account.");

        int howMany = db.getQuery("select * from Users  where authToken = ?", header.get(AUTH)).length;

        if(howMany > 1) {
            return new Response()
            .setStatus(500)
            .setError("Multiple users with the same Auth Token???")
            .setMessage("How did this even happen????");
        }
        else if(howMany == 0) {
            return new Response()
            .setStatus(404)
            .setError("No accounts found with your auth token.")
            .setMessage("The account you are trying to update doesn't exist.");
        }

        // If the "amt" is not a number then default to 0, same goes for -ve amounts.
        float addition = body.get("amt") instanceof Number x ? (x.floatValue() < 0 ? 0 : x.floatValue()) : 0;

        Account acc = getUserAccount(header.get(AUTH));
        acc.setBalance(acc.getBalance() + addition);
        int updated = db.updateQuery("update Users set balance = ? where authToken = ?", acc.getBalance(), acc.getAuthToken());

        if(updated == -1) {
            return new Response()
            .setStatus(updated)
            .setError("Trying to deposit to an account that doesn't exist")
            .setMessage("Way too much wrong here. 1) How were you able to login? 2) Why do you not have an account already?");
        }

        db.updateQuery("insert into Transactions values(?, ?, ?, ?, ?)", acc.getAuthToken(), "credit", addition, acc.getBalance(), LocalDateTime.now(ZoneId.of("UTC")));

        return new Response()
        .setStatus(200)
        .setMessage("Successfully added " + addition + " funds to your account.");
    }

    // FUNCTION NO. 3
    // Functionality Status: Works
    @PostMapping("/pay")
    public Response payUser(@RequestHeader HashMap<String, String> header, @RequestBody HashMap<String, Object> body) {
        Response res = checkAuthentication(header, "You need to provide an authorization token before being able deposit money.");
        if(res != null) return res;

        if(!body.containsKey("to")) {
            return new Response()
            .setStatus(400)
            .setError("Required information missing from request body.")
            .setMessage("The body of the request doesn't contain the destination of the funds.");
        }
        if(!body.containsKey("amt")) {
            return new Response()
            .setStatus(400)
            .setError("Required information missing from request body.")
            .setMessage("The body of the request doesn't contain the amount to be sent.");
        }
        
        float amount = 0;
        if(body.get("amt") instanceof Number x)
            amount = x.floatValue();

        if(amount < 0) {
            return new Response()
            .setStatus(400)
            .setError("Negative amount")
            .setMessage("You can't pay negative amount of funds.");
        }

        Account sender = getUserAccount(header.get(AUTH));
        if(sender == null){
            return new Response()
            .setStatus(409)
            .setError("Account doesn't exist.")
            .setMessage("The auth token used doesn't actually exist within the database.");
        }
        if(sender.getBalance() < amount){
            return new Response()
            .setStatus(400)
            .setMessage("Insufficient funds");
        }

        String rows[] = db.getQuery("select * from Users where username = ?", body.get("to"));
        if(rows == null || rows.length == 0){
            return new Response()
            .setStatus(404)
            .setError("Sending funds to non-existant account")
            .setMessage("The user you are trying to send funds to doesn't exist.");
        }

        Account reciever = gson.fromJson(rows[0], Account.class);

        if(reciever.equals(sender)) {
            return new Response()
            .setStatus(400)
            .setError("Sender and reciever are same")
            .setMessage("The sender and reciever of funds can't be the same individual.");
        }

        reciever.setBalance(reciever.getBalance() + amount);
        sender.setBalance(sender.getBalance() - amount);

        int updated = db.updateQuery("update Users set balance = ? where authToken = ?", sender.getBalance(), sender.getAuthToken());
        if(updated == -1) {
            return new Response().setStatus(500)
            .setError("Failed DB operation")
            .setMessage("There was a problem when trying to update sender's balance.");
        }

        updated = db.updateQuery("update Users set balance = ? where authToken = ?", reciever.getBalance(), reciever.getAuthToken());
        if(updated == -1) {
            db.updateQuery("update Users set balance = ? where authToken = ?", sender.getBalance() + amount, sender.getAuthToken());
            return new Response().setStatus(500)
            .setError("Failed DB operation")
            .setMessage("There was a problem when trying to update reciever's balance.");
        }
        //response.setBalance(amount);

        db.updateQuery("insert into Transactions values(?, ?, ?, ?, ?)", sender.getAuthToken(), "debit", amount, sender.getBalance(), LocalDateTime.now(ZoneId.of("UTC")));
        db.updateQuery("insert into Transactions values(?, ?, ?, ?, ?)", reciever.getAuthToken(), "credit", amount, reciever.getBalance(), LocalDateTime.now(ZoneId.of("UTC")));

        return new Response().setStatus(200)
        .setMessage("Successfully transferred funds.");
    }

    // FUNCTION NO. 4
    // Functionality Status: Works
    @GetMapping("/bal")
    public Balance getBalance(@RequestHeader HashMap<String, String> header, @RequestParam String currency) {
        Response res = checkAuthentication(header, "You need to have Authorization before being able to check the balance.");
        if(res != null) return null;

        
        Account acc = getUserAccount(header.get(AUTH));
        if(currency.equals("INR"))
            return new Balance()
            .setBalance(acc.getBalance())
            .setCurrency(currency);
        
        float balance = (float) convert(acc.getBalance(), "INR", currency);

        return new Balance()
        .setBalance(balance)
        .setCurrency(currency);
    }

    // FUNCTION NO. 5
    // Functionality Status: Works
    @GetMapping("/stmt")
    public ArrayList<Transaction> getTransactions(@RequestHeader HashMap<String, String> header) {
        if(checkAuthentication(header, "no need") != null)
            return null;

        ArrayList<Transaction> transactionList = new ArrayList<>();
        Account acc = getUserAccount(header.get(AUTH));

        String rows[] = db.getQuery("select kind, amount, updated_bal, timestamp from Transactions where authToken = ? order by timestamp desc", acc.getAuthToken());
        
        for(String row : rows)
            transactionList.add(gson.fromJson(row, Transaction.class));
        
        return transactionList;
    }

    // FUNCTION NO. 6
    // Functionality Status: Works.
    @PostMapping("/product")
    public Response postProduct(@RequestHeader HashMap<String, String> header, @RequestBody HashMap<String, Object> body) {
        Response res = checkAuthentication(header, "You need to provide an authorization token before being able deposit money.");
        if(res != null) return res;

        if(!body.containsKey("name")) {
            return new Response()
            .setStatus(400)
            .setError("Required information missing from request body")
            .setMessage("The NAME of the product is required to be able to add it to the database.");
        }
        else if(!body.containsKey("description")) {
            return new Response()
            .setStatus(400)
            .setError("Required information missing from request body")
            .setMessage("The DESCRIPTION of the product is required to be able to add it to the database.");
        }
        else if(!body.containsKey("price")) {
            return new Response()
            .setStatus(400)
            .setError("Required information missing from request body")
            .setMessage("The PRICE of the product is required to be able to add it to the database.");
        }

        Product add = new Product();
        if(body.get("price") instanceof Number x)
            add.setPrice(x.floatValue());

        if(add.getPrice() < 0) {
            return new Response()
            .setStatus(400)
            .setError("Negative Pricing")
            .setMessage("Negative price for a product is not valid.");
        }

        add.setDescription(body.get("description").toString())
        .setName(body.get("name").toString());

        // Do addition to DB, if successful return successResponse.
        String ret[] = db.getQuery("select * from Products;");
        int id[] = new int[ret.length];

        for(int i = 0; i < ret.length; i++)
            id[i] = gson.fromJson(ret[i], Product.class).getId();
        

        int computedId = getMissingNumber(id);
        int updated = db.updateQuery("insert into Products values(?, ?, ?, ?)", computedId, add.getName(), add.getPrice(), add.getDescription());
        
        if(updated > 0)
            return new Response()
            .setStatus(201)
            .setId(computedId)
            .setMessage("Product Added");
        else
            return new Response()
            .setStatus(500)
            .setError("Error when adding product to Database")
            .setMessage("There was an issue when adding the product to the database. Try again later.");
    }

    // FUNCTION NO. 7
    // Functionality Status: Works
    @GetMapping("/product")
    public ArrayList<Product> getAllProduct() {
        ArrayList<Product> productList = new ArrayList<>();

        String ret[] = db.getQuery("select * from Products");

        if(ret == null) return productList;

        for(String x : ret)
            productList.add(gson.fromJson(x, Product.class));

        return productList;
    }

    // FUNCITON NO. 8
    // Functionality Status: Works
    @PostMapping("/buy")
    public Response buyProduct(@RequestHeader HashMap<String, String> header, @RequestBody HashMap<String, Object> body){
        Response res = checkAuthentication(header, "You need to provide an authorization token before being able deposit money.");
        if(res != null) return res;

        if(!body.containsKey("product_id"))
            return new Response()
            .setError("Required information missing from request body")
            .setMessage("The id of the purchased product is missing.");

        Account acc = getUserAccount(header.get(AUTH));

        int requiredId = -1;
        if(body.get("product_id") instanceof Number n)
            requiredId = n.intValue();

        String rows[] = db.getQuery("select * from Products where id = ?", requiredId);

        if(rows == null || rows.length == 0)
            return new Response()
            .setStatus(404)
            .setError("Invalid product id")
            .setMessage("Make sure the product_id is correct.");

        Product product = gson.fromJson(rows[0], Product.class);
        
        if(acc.getBalance() < product.getPrice())
            return new Response()
            .setStatus(409)
            .setError("Insufficient funds")
            .setMessage("You will need to deposit more funds before being able to buy the product.");

        acc.setBalance(acc.getBalance() - product.getPrice());

        db.updateQuery("update Users set balance = ? where authToken = ?", acc.getBalance(), acc.getAuthToken());
        db.updateQuery("insert into Transactions values(?, ?, ?, ?, ?)", acc.getAuthToken(), "debit", product.getPrice(), acc.getBalance(), LocalDateTime.now(ZoneId.of("UTC")));
        
        if(db.deleteQuery("delete from Products where id = ?", product.getId()) == 0)
            return new Response()
            .setStatus(404)
            .setError("Product doesn't exist");

        return new BuyResponse()
        .setStatus(200)
        .setMessage("Product purchased.").setBalance(acc.getBalance());
    }

    // Functionality Status: Works
    @PostMapping("/login")
    public Response loginToSystem(@RequestBody HashMap<String, Object> body) {
        if(!body.containsKey("username"))
            return new Response()
            .setStatus(400)
            .setError("Required information missing from request body")
            .setMessage("You need to provide the USERNAME in order to be able to login.");
        if(!body.containsKey("password"))
            return new Response()
            .setStatus(400)
            .setError("Required information missing from request body")
            .setMessage("You need to provide the PASSWORD in order to be able to login.");

        String rows[] = db.getQuery("select password, authToken from Users where username = ?", body.get("username"));

        if(rows == null || rows.length == 0)
            return new Response()
            .setStatus(404)
            .setError("No valid username found")
            .setMessage("No such user was found. You can register first, and then login.");

        HashMap<String, Object> resultMap = gson.fromJson(rows[0], HashMap.class);
        String storedHash = (String) resultMap.get("password");
        String authToken = (String) resultMap.get("authToken");

        if(!BCrypt.checkpw(body.get("password").toString(), storedHash)) {
            return new Response()
            .setStatus(403)
            .setError("Incorrect Password")
            .setMessage("The password is wrong for the given username.");
        }

        if(session.addSession(authToken))
            return new Response()
            .setStatus(200)
            .setMessage("Login was successful. Your auth token = " + authToken);
        else
            return new Response()
            .setStatus(409)
            .setError("Session already active")
            .setMessage("This authorization is already under use.");
    }

    // functionality Status: Works
    @PostMapping("/terminate")
    public Response logoutSystem(@RequestHeader HashMap<String, String> header) {
        Response res = checkAuthentication(header, "No authorization found in header.");
        if(res != null) return res;

        if(session.removeSession(header.get(AUTH)))
            return new Response().setStatus(200)
            .setMessage("You were successfully logged out.");
        else
            return new Response().setStatus(400)
            .setError("No active session found")
            .setMessage("You don't have an active session with that authorization.");
    }

    private int getMissingNumber(int id[]) {
        if(id.length == 0 || id[0] != 1) return 1;

        java.util.Arrays.sort(id);

        for(int i = 0; i < id.length - 1; i++)
            if(id[i + 1] - id[i] > 1)
                return id[i] + 1;

        return id[id.length - 1] + 1;
    }

    public Account getUserAccount(String authToken) {
        String rows[] = db.getQuery("select * from Users where authToken = ?;", authToken);

        if(rows == null || rows.length == 0) return null;

        Account acc = gson.fromJson(rows[0], Account.class);
        return acc;
    }

    public double convert(double amount, String from, String to) {
        try {
            if(API_KEY == null) {
                String rows[] = db.getQuery("select api_key from ServerInfo");
                if(rows == null || rows.length == 0) return -1;

                Type type = new TypeToken<HashMap<String, Object>>() {}.getType();
                HashMap<String, Object> map = gson.fromJson(rows[0], type);
                Object ret = map.getOrDefault("api_key", null);

                if(ret == null) return Double.MIN_VALUE;

                API_KEY = ret.toString();
            }

            String url = String.format(
                "https://api.currencyapi.com/v3/latest?apikey=%s&base_currency=%s&currencies=%s",
                API_KEY, from, to
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            double rate = json.getAsJsonObject("data").getAsJsonObject(to).get("value").getAsDouble();

            return amount * rate;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}