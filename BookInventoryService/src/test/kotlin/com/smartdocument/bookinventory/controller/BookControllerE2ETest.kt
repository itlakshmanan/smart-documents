package com.smartdocument.bookinventory.controller

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.ValidatableResponse
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class BookControllerE2ETest {
    @LocalServerPort
    var port: Int = 0

    private val basePath = "/api/v1/books"
    private val username = "bookadmin"
    private val password = "bookpass123"

    @BeforeAll
    fun setUp() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
    }

    @Test
    fun `GET all books should return 200 and list`() {
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .`when`().get(basePath)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("size()", greaterThanOrEqualTo(0))
    }

    @Test
    fun `POST create book should return 200 and created book`() {
        val book = mapOf(
            "title" to "E2E Book",
            "author" to "E2E Author",
            "genre" to "Test",
            "isbn" to "978-1234567800",
            "price" to BigDecimal("19.99"),
            "quantity" to 10,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "2024-01-01",
            "description" to "E2E test book"
        )
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("title", equalTo("E2E Book"))
    }

    @Test
    fun `GET book by id should return 200 if exists`() {
        // First, create a book
        val book = mapOf(
            "title" to "E2E Book2",
            "author" to "E2E Author2",
            "genre" to "Test",
            "isbn" to "978-1234567801",
            "price" to BigDecimal("29.99"),
            "quantity" to 5,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "2024-01-02",
            "description" to "E2E test book 2"
        )
        val id = RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract().path<Int>("id")

        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .`when`().get("$basePath/$id")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(id))
    }

    @Test
    fun `PUT update book should return 200 and updated book`() {
        // Create a book
        val book = mapOf(
            "title" to "E2E Book3",
            "author" to "E2E Author3",
            "genre" to "Test",
            "isbn" to "978-1234567802",
            "price" to BigDecimal("39.99"),
            "quantity" to 7,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "2024-01-03",
            "description" to "E2E test book 3"
        )
        val id = RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .extract().path<Int>("id")

        val updatedBook = book.toMutableMap().apply { put("title", "Updated Title") }
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(updatedBook)
            .`when`().put("$basePath/$id")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("title", equalTo("Updated Title"))
    }

    @Test
    fun `PATCH update inventory should return 200 and updated quantity`() {
        // Create a book
        val book = mapOf(
            "title" to "E2E Book4",
            "author" to "E2E Author4",
            "genre" to "Test",
            "isbn" to "978-1234567803",
            "price" to BigDecimal("49.99"),
            "quantity" to 3,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "2024-01-04",
            "description" to "E2E test book 4"
        )
        val id = RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .extract().path<Int>("id")

        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .`when`().patch("$basePath/$id/inventory?quantity=99")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("quantity", equalTo(99))
    }

    @Test
    fun `DELETE book should return 204`() {
        // Create a book
        val book = mapOf(
            "title" to "E2E Book5",
            "author" to "E2E Author5",
            "genre" to "Test",
            "isbn" to "978-1234567804",
            "price" to BigDecimal("59.99"),
            "quantity" to 2,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "2024-01-05",
            "description" to "E2E test book 5"
        )
        val id = RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .extract().path<Int>("id")

        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .`when`().delete("$basePath/$id")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())
    }

    @Test
    fun `GET genres should return 200 and list`() {
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .`when`().get("$basePath/genres")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("size()", greaterThanOrEqualTo(0))
    }

    @Test
    fun `GET advanced search should return 200`() {
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .param("page", 0)
            .param("size", 10)
            .`when`().get("$basePath/advanced-search")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("content", notNullValue())
    }

    @Test
    fun `POST create book with missing required fields should return 400`() {
        val book = mapOf(
            "author" to "No Title",
            "genre" to "Test",
            "isbn" to "978-1234567805",
            "price" to BigDecimal("9.99"),
            "quantity" to 1,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "2024-01-01"
        )
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `POST create book with invalid ISBN should return 400`() {
        val book = mapOf(
            "title" to "Invalid ISBN",
            "author" to "E2E Author",
            "genre" to "Test",
            "isbn" to "INVALIDISBN",
            "price" to BigDecimal("9.99"),
            "quantity" to 1,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "2024-01-01"
        )
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `POST create book with negative price should return 400`() {
        val book = mapOf(
            "title" to "Negative Price",
            "author" to "E2E Author",
            "genre" to "Test",
            "isbn" to "978-1234567806",
            "price" to BigDecimal("-1.00"),
            "quantity" to 1,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "2024-01-01"
        )
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `POST create book with zero quantity should return 200`() {
        val book = mapOf(
            "title" to "Zero Quantity",
            "author" to "E2E Author",
            "genre" to "Test",
            "isbn" to "978-1234567807",
            "price" to BigDecimal("1.00"),
            "quantity" to 0,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "2024-01-01"
        )
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .statusCode(HttpStatus.OK.value())
    }

    @Test
    fun `POST create book with too long title should return 400`() {
        val book = mapOf(
            "title" to "A".repeat(300),
            "author" to "E2E Author",
            "genre" to "Test",
            "isbn" to "978-1234567808",
            "price" to BigDecimal("1.00"),
            "quantity" to 1,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "2024-01-01"
        )
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `POST create book with blank fields should return 400`() {
        val book = mapOf(
            "title" to "",
            "author" to "",
            "genre" to "",
            "isbn" to "",
            "price" to BigDecimal("1.00"),
            "quantity" to 1,
            "language" to "",
            "publisher" to "",
            "publishedDate" to ""
        )
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `POST create book with invalid date should return 400`() {
        val book = mapOf(
            "title" to "Invalid Date",
            "author" to "E2E Author",
            "genre" to "Test",
            "isbn" to "978-1234567809",
            "price" to BigDecimal("1.00"),
            "quantity" to 1,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "01-01-2024"
        )
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `POST create duplicate ISBN should return 409`() {
        val book = mapOf(
            "title" to "Duplicate ISBN",
            "author" to "E2E Author",
            "genre" to "Test",
            "isbn" to "978-1234567899",
            "price" to BigDecimal("1.00"),
            "quantity" to 1,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "2024-01-01"
        )
        // Create first book
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .statusCode(HttpStatus.OK.value())
        // Try to create duplicate
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .statusCode(HttpStatus.CONFLICT.value())
    }

    @Test
    fun `GET book by non-existent id should return 404`() {
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .`when`().get("$basePath/999999")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
    }

    @Test
    fun `PUT update non-existent book should return 404`() {
        val book = mapOf(
            "title" to "Update Not Found",
            "author" to "E2E Author",
            "genre" to "Test",
            "isbn" to "978-1234567810",
            "price" to BigDecimal("1.00"),
            "quantity" to 1,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "2024-01-01"
        )
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().put("$basePath/999999")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
    }

    @Test
    fun `PATCH update inventory to negative should return 400`() {
        // Create a book
        val book = mapOf(
            "title" to "Negative Inventory",
            "author" to "E2E Author",
            "genre" to "Test",
            "isbn" to "978-1234567811",
            "price" to BigDecimal("1.00"),
            "quantity" to 1,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "2024-01-01"
        )
        val id = RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .extract().path<Int>("id")
        // Try to update inventory to negative
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .`when`().patch("$basePath/$id/inventory?quantity=-5")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `DELETE non-existent book should return 404`() {
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .`when`().delete("$basePath/999999")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
    }

    @Test
    fun `DELETE book then GET should return 404`() {
        // Create a book
        val book = mapOf(
            "title" to "Delete Then Get",
            "author" to "E2E Author",
            "genre" to "Test",
            "isbn" to "978-1234567812",
            "price" to BigDecimal("1.00"),
            "quantity" to 1,
            "language" to "English",
            "publisher" to "E2E Publisher",
            "publishedDate" to "2024-01-01"
        )
        val id = RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .extract().path<Int>("id")
        // Delete
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .`when`().delete("$basePath/$id")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())
        // Try to get
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .`when`().get("$basePath/$id")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
    }

    @Test
    fun `GET advanced search with no matches should return empty content`() {
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .param("title", "NoSuchBookTitle")
            .param("page", 0)
            .param("size", 10)
            .`when`().get("$basePath/advanced-search")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("content.size()", equalTo(0))
    }

    @Test
    fun `POST create book with special characters and unicode should return 200`() {
        val book = mapOf(
            "title" to "Tést!@# \u001f600 \u001f601 \u001f602",
            "author" to "Äüößçñ!@# \u001f600",
            "genre" to "Fiction",
            "isbn" to "978-1234567813",
            "price" to BigDecimal("1.00"),
            "quantity" to 1,
            "language" to "日本語",
            "publisher" to "Pub!@# \u001f600",
            "publishedDate" to "2024-01-01",
            "description" to null
        )
        RestAssured.given()
            .auth().preemptive().basic(username, password)
            .contentType(ContentType.JSON)
            .body(book)
            .`when`().post(basePath)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("title", containsString("Tést!@#"))
    }

    @Test
    fun `GET all books without authentication should return 401`() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .`when`().get(basePath)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
    }

    @Test
    fun `GET all books with wrong credentials should return 401`() {
        RestAssured.given()
            .auth().preemptive().basic("wronguser", "wrongpass")
            .contentType(ContentType.JSON)
            .`when`().get(basePath)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
    }

    @Test
    fun `GET actuator health without authentication should return 200`() {
        RestAssured.given()
            .`when`().get("/actuator/health")
            .then()
            .statusCode(HttpStatus.OK.value())
    }
}
