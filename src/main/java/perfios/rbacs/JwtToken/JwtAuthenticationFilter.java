package perfios.rbacs.JwtToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import perfios.rbacs.Model.LoginPost.LoginPostOb;
import perfios.rbacs.RbacsApplication;
import perfios.rbacs.Repository.UserRepository.UserService;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    @Autowired
    JwtTokenService jwtTokenService;

    @Autowired
    UserService userService;
    UserDetails userDetails;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(SecurityContextHolder.getContext() != null) RbacsApplication.printString("--------" + SecurityContextHolder.getContext().toString());


        if(request.getRequestURI().startsWith("/loginjwt")) {
            filterChain.doFilter(request,response);
            return;
        }

        if(request.getRequestURI().startsWith("/checking")){
            filterChain.doFilter(request,response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }


        String jwt = authHeader.substring(7);
        RbacsApplication.printString("jwt token = " + jwt);

        if(!jwtTokenService.checkValidityOfJwtToken(jwt)) {
            RbacsApplication.printString("Token is Invalid !!!!!!!!!!!!");
            RbacsApplication.printString(SecurityContextHolder.getContext().toString());
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request,response);
            return;
        }

        int userId = jwtTokenService.extractUserIdFromJwtToken(jwt);

        int roleId = jwtTokenService.getUserRoleId(jwt);

        if(userId != -1 && roleId != -1 && SecurityContextHolder.getContext().getAuthentication() == null){
            LoginPostOb loginPostOb = userService.fetchUserDetailFromUserId(userId);
            if(loginPostOb == null) {
                return;
            }

            List<GrantedAuthority> authorities = jwtTokenService.getAllUserAuthorities(jwt);
            RbacsApplication.printString("role_id = " + roleId);
            authorities = jwtTokenService.addRoleInAuthorities(authorities,roleId);
            userDetails = new User(
                    loginPostOb.getUserEmail(),
                    loginPostOb.getUserPassword(),
                    authorities
            );

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userDetails, loginPostOb.getUserPassword(), userDetails.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            RbacsApplication.printString("printing securitycontextholder after verification of jwt = \n" + SecurityContextHolder.getContext());
            filterChain.doFilter(request,response);
            return;
        }
        filterChain.doFilter(request,response);
        return;
    }
}