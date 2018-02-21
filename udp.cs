using System;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Timers;
using System.Diagnostics;

public class UdpSrvrSample
{
   public static void Main()
   {

      if(checkConnection("192.168.0.80", 7801))
      {
         System.Timers.Timer aTimer = new System.Timers.Timer();
         aTimer.Elapsed+=new ElapsedEventHandler(OnTimedEvent);
         aTimer.Interval=300;
         aTimer.Enabled=true;
      }
      else{

         Console.WriteLine("Connection is unavailable");
      }

      Console.WriteLine("Press \'q\' to quit the sample.");
      while(Console.Read()!='q');
   }

   static bool checkConnection(string address, int port){
      // This method is checking if the server is ready to receive UDP packets.
      // It is taking as parameter the address and the port of the server we are working with


      IPEndPoint endPoint = new IPEndPoint(IPAddress.Parse(address), port);   
      Socket sock = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);

      byte[] send_buffer = Encoding.ASCII.GetBytes("CheckConnection");
      sock.SendTo(send_buffer , endPoint);

      byte[] response = Encoding.ASCII.GetBytes("");
      
      sock.ReceiveTimeout = 1000;
      try{
         sock.Receive(response);
      }
      catch(Exception e){
         return false;
      }

      return true;

   }

   static void sendUdp(string address, int port, string message){
      // This method is sending the message at the address and the port it receives as parameters
      // use this with the location on each tick
      
      IPEndPoint endPoint = new IPEndPoint(IPAddress.Parse(address), port);   
      Socket sock = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);

      byte[] send_buffer = Encoding.ASCII.GetBytes(message);
      sock.SendTo(send_buffer , endPoint);
   }

   private static void OnTimedEvent(object source, ElapsedEventArgs e){
      // This method is just to evaluate the 300ms running on my machine
      
      string message = System.IO.File.ReadAllText(@"location.txt");
      sendUdp("192.168.0.80", 7801, message);
      Console.WriteLine("Sent!");
   }

}