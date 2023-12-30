package app.security;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import app.models.Customer.Customer;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenUtil implements Serializable {

    private static final long serialVersionUID = -2550185165626007488L;

    public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60;

    @Value("${jwt.secret}")
    private String secret;

    //retrieve username from jwt token
    public String getUsernameFromToken(String token) throws GlobalException {
        return getClaimFromToken(token, Claims::getSubject);
    }

    //retrieve expiration date from jwt token
    public Date getExpirationDateFromToken(String token) throws GlobalException {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) throws GlobalException {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    //for retrieveing any information from token we will need the secret key
    private Claims getAllClaimsFromToken(String token) throws GlobalException {
        if (token == null)
            throw new GlobalException("Token is null", HttpStatus.UNAUTHORIZED);
        Jwts.parser();
//        JwtParser parser = Jwts.parser().setSigningKey(secret).build();
        try {
//            Jws<Claims> x = parser.parseClaimsJws(token);
//            return x.getBody();
        } catch (ExpiredJwtException e) {
            throw new GlobalException("Token is expired", HttpStatus.UNAUTHORIZED);
        } catch (UnsupportedJwtException e) {
            throw new GlobalException("Token is unsupported", HttpStatus.UNAUTHORIZED);
        } catch (MalformedJwtException e) {
            throw new GlobalException("Token is malformed", HttpStatus.UNAUTHORIZED);
        } catch (SignatureException e) {
            throw new GlobalException("Token is invalid", HttpStatus.UNAUTHORIZED);
        } catch (IllegalArgumentException e) {
            throw new GlobalException("Token is null", HttpStatus.UNAUTHORIZED);
        }
        throw new GlobalException("Token is null", HttpStatus.UNAUTHORIZED);
    }

    //check if the token has expired
    private Boolean isTokenExpired(String token) throws GlobalException {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    //generate token for user
    public String generateToken(Customer userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return doGenerateToken(claims, userDetails.getUsername());
    }

    //while creating the token -
    //1. Define  claims of the token, like Issuer, Expiration, Subject, and the ID
    //2. Sign the JWT using the HS512 algorithm and secret key.
    //3. According to JWS Compact Serialization(https://tools.ietf.org/html/draft-ietf-jose-json-web-signature-41#section-3.1)
    //   compaction of the JWT to a URL-safe string
    private String doGenerateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
                .signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    //validate token
    public Boolean validateToken(String token, Customer userDetails) throws GlobalException {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}