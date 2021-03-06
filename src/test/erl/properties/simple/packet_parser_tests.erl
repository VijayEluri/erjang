-module(packet_parser_tests).

-compile(export_all).

-include("triq.hrl").
-include("unit.hrl").

% ../../../../../jerl -sname erj -pz ../../../../../triq/ebin
% c(packet_parser_tests, [{i, "../../../../../triq/include"}]).
% packet_parser_tests:test().


%%%========== Entry point: ====================
test() ->
    eunit_test(?MODULE).

%%%========== Tests: ==============================

http_parser_wholesend_test() ->
    do_test_http_parser(250, fun gen_tcp:send/2).

http_parser_splitsend_test() ->
    do_test_http_parser(250, fun send_piecemeal/2).

do_test_http_parser(N, SendFun) ->

    %% Create server end:
    {SS, Port} = create_server_socket([{packet,http_bin}]),
    Gatherer = create_gather_process(SS, self()),

    %% Create client end:
    spawn_clients(Port, N,
                  fun(CS) ->
                          Request = "GET /foo HTTP/1.1\r\nHeader1: Value1\r\nVerylongheadernameyesindeed: And then some long value, just like, you, know, ...\r\n\r\nBody",
                          ok = SendFun(CS, Request),
                          ok = gen_tcp:close(CS)
                  end),
    timer:sleep(1000),
    Msgs = flush(),
    lists:foreach(fun(M) ->
                          ?assertMatch([{http_request,'GET',{abs_path, <<"/foo">>}, {1,1}},
                                        {http_header,_,<<"Header1">>,_,<<"Value1">>},
                                        {http_header,_,<<"Verylongheadernameyesindeed">>,_,<<"And then some long value, just like, you, know, ...">>},
                                        http_eoh],
                                       M),
                          io:format(":) "),
                          true
                     end,
                  Msgs),
    ?assertEqual(N, length(Msgs)),
    unlink(Gatherer), exit(Gatherer, shutdown).

spawn_clients(Port, N, ClientAction)
  when is_integer(Port),
       is_integer(N),
       is_function(ClientAction,1) ->
    lists:foreach(fun(_) ->
                          {ok,CS} = gen_tcp:connect("localhost",Port, [binary]),
                          ClientAction(CS)
                  end,
                  lists:seq(1,N)).


%%%========== Utility: ==============================
create_server_socket(Opts) ->
    {ok,S} = gen_tcp:listen(0, [{active,false} | Opts]),
    {ok,Port} = inet:port(S),
    {S, Port}.

create_gather_process(ServerSocket, Dest) ->
    link(Dest),
    spawn_link(fun() ->
                       case gen_tcp:accept(ServerSocket, 1000) of
                           {ok,S} ->
                               spawn_link(fun() -> create_gather_process(ServerSocket, Dest) end),
                               gather_then_send(S,Dest);
                           {error,_Err} ->
                               io:format("DB| Server: error ~p\n", [_Err]),
                               gen_tcp:close(ServerSocket),
                               ok % Done.
                       end
               end).

gather_then_send(Socket, Dest) ->
    gather_then_send(Socket, Dest, []).

gather_then_send(Socket, Dest, Acc) ->
    case gen_tcp:recv(Socket,0,1000) of
        {ok,Data} ->
            %% io:format("DB| Gatherer: got data: ~p\n", [Data]),
            gather_then_send(Socket, Dest, [Data|Acc]);
        {error,Err} ->
            %% io:format("DB| Gatherer: got error: ~p\n", [Err]),
            gen_tcp:close(Socket),
            Acc2 = case Err of
                       closed -> Acc;
                       _ -> [{recv_error,Err}|Acc]
                   end,
            Dest ! lists:reverse(Acc2)
    end.

flush() ->
    receive M -> [M|flush()]
    after 0 -> []
    end.

send_piecemeal(_S, []) -> ok;
send_piecemeal(S, String) ->
    {_,_,X} = now(),
    N = 1+(X rem length(String)),
    {Now,Later} = lists:split(N, String),
    gen_tcp:send(S, Now),
    send_piecemeal(S, Later).
